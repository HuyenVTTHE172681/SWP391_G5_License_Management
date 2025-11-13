package swp391.fa25.lms.service.customer;

import com.paypal.api.payments.*;
import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.PayPalRESTException;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import swp391.fa25.lms.model.*;
import swp391.fa25.lms.repository.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class PaymentPayPalService {

    @Value("${paypal.clientId}")
    private String clientId;
    @Value("${paypal.secret}")
    private String clientSecret;
    @Value("${paypal.mode}")
    private String mode;
    @Value("${paypal.returnUrl}")
    private String returnUrl;
    @Value("${paypal.cancelUrl}")
    private String cancelUrl;

    @Autowired
    private CustomerOrderRepository orderRepository;
    @Autowired
    private WalletTransactionRepository transactionRepository;
    @Autowired
    private ToolRepository toolRepository;
    @Autowired
    private LicenseToolRepository licenseToolRepository;
    @Autowired
    private LicenseAccountRepository licenseAccountRepository;
    @Autowired
    private WalletRepository walletRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private JavaMailSender mailSender;

    private APIContext apiContext() {
        return new APIContext(clientId, clientSecret, mode);
    }

    /**
     * Tạo payment URL (PayPal redirect)
     */
    @Transactional
    public String createPayment(Long toolId, Long licenseId, Account buyer, HttpServletRequest request) {
        try {
            Tool tool = toolRepository.findById(toolId).orElseThrow();
            License license = licenseToolRepository.findById(licenseId).orElseThrow();

            // Tạo wallet cho buyer nếu chưa có
            if (buyer.getWallet() == null) {
                Wallet buyerWallet = new Wallet();
                buyerWallet.setAccount(buyer);
                buyerWallet.setBalance(BigDecimal.ZERO);
                buyerWallet.setCurrency("USD");  // PayPal v1 dùng USD, sandbox convert VND
                buyerWallet.setUpdatedAt(LocalDateTime.now());
                walletRepository.save(buyerWallet);
                buyer.setWallet(buyerWallet);
                accountRepository.save(buyer);
                System.out.println("Created default wallet for buyer: " + buyer.getEmail());
            }

            // Tạo order PENDING
            CustomerOrder order = new CustomerOrder();
            order.setAccount(buyer);
            order.setTool(tool);
            order.setLicense(license);
            order.setPrice(license.getPrice() == null ? 0.0 : license.getPrice());
            order.setPaymentMethod(CustomerOrder.PaymentMethod.PAYPAL);  // Enum PAYPAL nếu có
            order.setOrderStatus(CustomerOrder.OrderStatus.PENDING);
            order.setCreatedAt(LocalDateTime.now());
            orderRepository.save(order);
            System.out.println("Created PENDING order ID: " + order.getOrderId() + " for tool: " + tool.getToolName());

            // Tạo transaction PENDING
            WalletTransaction tx = new WalletTransaction();
            tx.setWallet(buyer.getWallet());
            tx.setTransactionType(WalletTransaction.TransactionType.BUY);
            tx.setStatus(WalletTransaction.TransactionStatus.PENDING);
            tx.setAmount(BigDecimal.valueOf(order.getPrice()));
            tx.setCreatedAt(LocalDateTime.now());
            transactionRepository.save(tx);
            order.setTransaction(tx);
            orderRepository.save(order);

            // Tạo PayPal payment
            Amount amount = new Amount();
            amount.setCurrency("USD");
            amount.setTotal(String.format("%.2f", order.getPrice())); // PayPal yêu cầu định dạng xx.xx

            Transaction transaction = new Transaction();
            transaction.setDescription("Order ID: " + order.getOrderId());
            transaction.setAmount(amount);

            List<Transaction> transactions = new ArrayList<>();
            transactions.add(transaction);

            Payer payer = new Payer();
            payer.setPaymentMethod("paypal");

            Payment payment = new Payment();
            payment.setIntent("sale");
            payment.setPayer(payer);
            payment.setTransactions(transactions);

            RedirectUrls redirectUrls = new RedirectUrls();
            redirectUrls.setCancelUrl(cancelUrl + "?orderId=" + order.getOrderId());
            redirectUrls.setReturnUrl(returnUrl + "?orderId=" + order.getOrderId());
            payment.setRedirectUrls(redirectUrls);

            Payment createdPayment = payment.create(apiContext());

            // Lấy approval link
            for (Links link : createdPayment.getLinks()) {
                if (link.getRel().equalsIgnoreCase("approval_url")) {
                    return link.getHref();
                }
            }

            throw new RuntimeException("Không tìm thấy approval URL từ PayPal");
        } catch (PayPalRESTException e) {
            throw new RuntimeException("Lỗi tạo payment PayPal: " + e.getMessage(), e);
        }
    }

    /**
     * Xử lý callback sau khi user đồng ý thanh toán: check success → update + license + email
     *
     */
    @Transactional
    public boolean executePayment(String paymentId, String payerId, Long orderId) {
        try {
            Payment payment = new Payment();
            payment.setId(paymentId);
            PaymentExecution paymentExecution = new PaymentExecution();
            paymentExecution.setPayerId(payerId);
            Payment executedPayment = payment.execute(apiContext(), paymentExecution);

            boolean success = "approved".equalsIgnoreCase(executedPayment.getState());  // approved sau execute

            if (success) {
                Optional<CustomerOrder> optionalOrder = orderRepository.findById(orderId);
                if (optionalOrder.isEmpty()) {
                    System.err.println("Order not found: " + orderId);
                    return false;
                }
                CustomerOrder order = optionalOrder.get();
                System.out.println("Matched orderId: " + order.getOrderId() + " (PayPal state: " + executedPayment.getState() + ")");

                WalletTransaction tx = order.getTransaction();
                if (tx == null) {
                    System.err.println("No transaction linked to order " + orderId);
                    return false;
                }

                // Update transaction
                tx.setStatus(success ? WalletTransaction.TransactionStatus.SUCCESS : WalletTransaction.TransactionStatus.FAILED);
                tx.setUpdatedAt(LocalDateTime.now());
                transactionRepository.save(tx);

                // Update order + business logic (từ VNPay)
                order.setOrderStatus(success ? CustomerOrder.OrderStatus.SUCCESS : CustomerOrder.OrderStatus.FAILED);
                order.setUpdatedAt(LocalDateTime.now());
                orderRepository.save(order);

                if (success) {
                    // Cộng tiền cho seller (từ VNPay)
                    double amount = Double.parseDouble(executedPayment.getTransactions().get(0).getAmount().getTotal());
                    Account seller = order.getTool().getSeller();
                    Optional<Wallet> sellerWalletOpt = walletRepository.findByAccount(seller);
                    if (sellerWalletOpt.isPresent()) {
                        Wallet sellerWallet = sellerWalletOpt.get();
                        if (sellerWallet.getBalance() == null) sellerWallet.setBalance(BigDecimal.ZERO);
                        sellerWallet.setBalance(sellerWallet.getBalance().add(BigDecimal.valueOf(amount)));
                        sellerWallet.setUpdatedAt(LocalDateTime.now());
                        walletRepository.save(sellerWallet);
                        System.out.println("Updated seller wallet balance: " + sellerWallet.getBalance());
                    } else {
                        System.err.println("No wallet for seller: " + seller.getEmail());
                    }

                    // Giảm quantity tool
                    Tool tool = order.getTool();
                    if (tool.getQuantity() > 0) {
                        tool.setQuantity(tool.getQuantity() - 1);
                        toolRepository.save(tool);
                        System.out.println("Decreased quantity for tool " + tool.getToolId() + " to " + tool.getQuantity());
                    }

                    // THÊM: Tạo license account + gửi email
                    createAndAssignLicense(order);
                } else {
                    String message = executedPayment.getFailureReason() != null ? executedPayment.getFailureReason() : "Unknown error";
                    System.err.println("Payment failed for order " + order.getOrderId() + ": " + message);
                }

                System.out.println("Updated order " + order.getOrderId() + " to status: " + order.getOrderStatus());
                return success;
            }
            return false;
        } catch (PayPalRESTException e) {
            System.err.println("Execute payment error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Transactional
    public void cancelPayment(Long orderId) {
        Optional<CustomerOrder> optionalOrder = orderRepository.findById(orderId);
        if (optionalOrder.isPresent()) {
            CustomerOrder order = optionalOrder.get();
            order.setOrderStatus(CustomerOrder.OrderStatus.FAILED);  // Enum CANCELLED nếu có, hoặc FAILED
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);

            // Reset transaction nếu PENDING
            WalletTransaction tx = order.getTransaction();
            if (tx != null && tx.getStatus() == WalletTransaction.TransactionStatus.PENDING) {
                tx.setStatus(WalletTransaction.TransactionStatus.FAILED);
                tx.setUpdatedAt(LocalDateTime.now());
                transactionRepository.save(tx);
            }

            System.out.println("Cancelled order " + orderId + " - status: " + order.getOrderStatus());
        } else {
            System.err.println("Order not found for cancel: " + orderId);
        }
    }

    // Retry
    @Transactional
    public String createPaymentForRetry(Long orderId, Long licenseId, Account buyer, HttpServletRequest request) {
        CustomerOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order không tồn tại"));
        if (order.getOrderStatus() != CustomerOrder.OrderStatus.PENDING && order.getOrderStatus() != CustomerOrder.OrderStatus.FAILED) {
            throw new RuntimeException("Chỉ có thể retry order PENDING/FAILED!");
        }

        // Cập nhật license/price nếu thay đổi (từ VNPay)
        License license = licenseToolRepository.findById(licenseId).orElseThrow();
        order.setLicense(license);
        order.setPrice(license.getPrice() == null ? 0.0 : license.getPrice());
        orderRepository.save(order);

        // Reset transaction PENDING (nếu FAILED thì tạo mới, từ VNPay)
        WalletTransaction tx = order.getTransaction();
        if (tx == null || tx.getStatus() == WalletTransaction.TransactionStatus.FAILED) {
            tx = new WalletTransaction();
            tx.setWallet(buyer.getWallet());
            tx.setTransactionType(WalletTransaction.TransactionType.BUY);
            tx.setStatus(WalletTransaction.TransactionStatus.PENDING);
            tx.setAmount(BigDecimal.valueOf(order.getPrice()));
            tx.setCreatedAt(LocalDateTime.now());
            transactionRepository.save(tx);
            order.setTransaction(tx);
        } else {
            tx.setStatus(WalletTransaction.TransactionStatus.PENDING);
            tx.setAmount(BigDecimal.valueOf(order.getPrice()));
            tx.setCreatedAt(LocalDateTime.now());
            transactionRepository.save(tx);
        }
        orderRepository.save(order);

        // Tạo PayPal payment mới (PayPal tự unique ID, không cần ref như VNPay)
        System.out.println("Retry PayPal payment for orderId: " + orderId);
        return createPayment(order.getTool().getToolId(), licenseId, buyer, request);  // Gọi createPayment để tạo mới
    }

    // Sen email: Tách tạo license account
    private void createAndAssignLicense(CustomerOrder order) {
        Tool tool = order.getTool();
        License license = order.getLicense();
        Account buyer = order.getAccount();
        String loginMethod = tool.getLoginMethod().toString();
        if ("USER_PASSWORD".equals(loginMethod)) {
            // Tạo mới LicenseAccount cho USER_PASSWORD
            LicenseAccount acc = new LicenseAccount();
            acc.setLicense(license);
            acc.setOrder(order);
            acc.setUsed(true);
            acc.setStatus(LicenseAccount.Status.ACTIVE);
            acc.setStartDate(LocalDateTime.now());
            acc.setEndDate(LocalDateTime.now().plusDays(license.getDurationDays()));
            acc.setUsername("user_" + buyer.getAccountId() + "_" + System.currentTimeMillis());
            acc.setPassword(UUID.randomUUID().toString().substring(0, 8));
            licenseAccountRepository.save(acc);
            sendUserPasswordEmail(order, acc);
        } else if ("TOKEN".equals(loginMethod)) {
            try {
                // Debug count (giữ nguyên)
                long unusedCount = licenseAccountRepository.findByLicense_Tool_ToolId(tool.getToolId())
                        .stream()
                        .filter(acc -> !acc.getUsed())
                        .count();
                if (unusedCount == 0) {
                    throw new RuntimeException("No unused tokens available for tool " + tool.getToolId());
                }
                Optional<LicenseAccount> unusedToken = licenseAccountRepository.findFirstByLicense_Tool_ToolIdAndUsedFalse(tool.getToolId());
                if (unusedToken.isPresent()) {
                    LicenseAccount tokenAcc = unusedToken.get();
                    // Assign/save/mail (giữ nguyên)
                    tokenAcc.setLicense(license);
                    tokenAcc.setOrder(order);
                    tokenAcc.setUsed(true);
                    tokenAcc.setStatus(LicenseAccount.Status.ACTIVE);
                    tokenAcc.setStartDate(LocalDateTime.now());
                    tokenAcc.setEndDate(LocalDateTime.now().plusDays(license.getDurationDays()));
                    LicenseAccount savedAcc = licenseAccountRepository.save(tokenAcc);
                    sendTokenEmail(order, savedAcc);
                } else {
                    throw new RuntimeException("No unused token returned from query for toolId " + tool.getToolId());
                }
            } catch (Exception e) {
                System.err.println("Error assigning TOKEN for order " + order.getOrderId() + ": " + e.getMessage());
                e.printStackTrace();
                throw e;  // Rollback
            }
        }
    }

    // VNPay - Gửi mail USER_PASSWORD
    private void sendUserPasswordEmail(CustomerOrder order, LicenseAccount acc) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(order.getAccount().getEmail());
            helper.setSubject("[LMS] Thanh toán thành công - Thông tin tài khoản Tool");
            String body = """
                    <h2>Bạn đã mua thành công tool: <b>%s</b></h2>
                    <p><b>License:</b> %s</p>
                    <p><b>Tên đăng nhập:</b> %s</p>
                    <p><b>Mật khẩu:</b> %s</p>
                    <p>Thời hạn sử dụng đến: %s</p>
                    <br/>
                    <p>Chúc bạn trải nghiệm vui vẻ!</p>
                    """.formatted(
                    order.getTool().getToolName(),
                    order.getLicense().getName(),
                    acc.getUsername(),
                    acc.getPassword(),
                    acc.getEndDate().toLocalDate()
            );
            helper.setText(body, true);
            mailSender.send(message);
            System.out.println("Email USER_PASSWORD sent to " + order.getAccount().getEmail());
        } catch (MessagingException e) {
            System.err.println("Gửi email USER_PASSWORD thất bại: " + e.getMessage());
        }
    }

    // THÊM: Copy nguyên từ VNPay - Gửi mail TOKEN
    private void sendTokenEmail(CustomerOrder order, LicenseAccount tokenAcc) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(order.getAccount().getEmail());
            helper.setSubject("[LMS] Thanh toán thành công - Tool Token");
            String body = """
                    <h2>Bạn đã mua thành công tool: <b>%s</b></h2>
                    <p><b>License:</b> %s</p>
                    <p><b>Token sử dụng:</b> %s</p>
                    <p><b>Trạng thái token:</b> Đã kích hoạt</p>
                    <p>Thời hạn sử dụng đến: %s</p>
                    <br/>
                    <p><i>Lưu ý:</i> Token này chỉ được dùng một lần và không thể chỉnh sửa.</p>
                    """.formatted(
                    order.getTool().getToolName(),
                    order.getLicense().getName(),
                    tokenAcc.getToken(),
                    tokenAcc.getEndDate().toLocalDate()
            );
            helper.setText(body, true);
            mailSender.send(message);
            System.out.println("Email TOKEN sent to " + order.getAccount().getEmail());
        } catch (MessagingException e) {
            System.err.println("Gửi email TOKEN thất bại: " + e.getMessage());
        }
    }
}