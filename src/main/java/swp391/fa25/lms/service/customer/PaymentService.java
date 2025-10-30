package swp391.fa25.lms.service.customer;

import jakarta.mail.internet.MimeMessage;
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
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class PaymentService {
    @Autowired
    private ToolRepository toolRepository;
    @Autowired
    private LicenseToolRepository licenseToolRepository;
    @Autowired
    private CustomerOrderRepository orderRepository;
    @Autowired
    private WalletTransactionRepository transactionRepository;
    @Autowired
    private LicenseAccountRepository licenseAccountRepository;
    @Autowired
    private JavaMailSender mailSender;
    @Autowired
    private WalletRepository walletRepository;
    @Autowired
    private AccountRepository accountRepository;

    // Các biến môi trường VNPay (được config trong application.properties)
    @Value("${vnpay.tmnCode}")
    private String tmnCode;
    @Value("${vnpay.hashSecret}")
    private String hashSecret;
    @Value("${vnpay.baseUrl}")
    private String baseUrl;
    @Value("${vnpay.returnUrl}")
    private String returnUrl;

    /**
     * Tạo URL thanh toán VNPay: Tạo order PENDING + transaction PENDING trước
     */
    @Transactional
    public String createPaymentUrl(Long toolId, Long licenseId, Account buyer, HttpServletRequest request) {
        Tool tool = toolRepository.findById(toolId).orElseThrow();
        License license = licenseToolRepository.findById(licenseId).orElseThrow();

        // Auto tạo wallet cho buyer nếu chưa có
        if (buyer.getWallet() == null) {
            Wallet buyerWallet = new Wallet();
            buyerWallet.setAccount(buyer);
            buyerWallet.setBalance(BigDecimal.ZERO);
            buyerWallet.setCurrency("VND");
//            buyerWallet.setCreatedAt(LocalDateTime.now());
            buyerWallet.setUpdatedAt(LocalDateTime.now());
            walletRepository.save(buyerWallet);
            buyer.setWallet(buyerWallet);
            accountRepository.save(buyer);
            System.out.println("Created default wallet for buyer: " + buyer.getEmail());
        }

        // Tạo order PENDING trước thanh toán
        CustomerOrder order = new CustomerOrder();
        order.setAccount(buyer);
        order.setTool(tool);
        order.setLicense(license);
        order.setPrice(license.getPrice() == null ? 0.0 : license.getPrice());
        order.setPaymentMethod(CustomerOrder.PaymentMethod.BANK);
        order.setOrderStatus(CustomerOrder.OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());
        orderRepository.save(order);  // Lưu order với ID

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

        // Tạo txnRef giống C#: DateTime.Now.Ticks (unique timestamp)
        String txnRef = String.valueOf(System.currentTimeMillis());  // Equivalent to Ticks, unique

        // OrderInfo cho initial: toolId_licenseId_buyerId (giống C# OrderInfo = codePayment, nhưng dùng orderId)
        String orderInfo = String.valueOf(order.getOrderId());

        // Tạo params VNPay (giống C# AddRequestData)
        long amountInt = Math.round(order.getPrice() * 100);

        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", "2.1.0");  // Fixed version như C#
        vnpParams.put("vnp_Command", "pay");
        vnpParams.put("vnp_TmnCode", tmnCode.trim());
        vnpParams.put("vnp_Amount", String.valueOf(amountInt));
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_TxnRef", txnRef);  // Unique timestamp
        vnpParams.put("vnp_OrderInfo", orderInfo);
        vnpParams.put("vnp_OrderType", "billpayment");  // Giống C# TypePayment
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", returnUrl.trim());
        vnpParams.put("vnp_IpAddr", request.getRemoteAddr());
        vnpParams.put("vnp_CreateDate", new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));

        // Build hash/query (giống C# vnpay.CreateRequestUrl)
        String hashDataStr = buildVnpHashData(vnpParams);
        String queryStr = buildVnpQuery(vnpParams);

        String secureHash = hmacSHA512(hashSecret.trim(), hashDataStr).toUpperCase(Locale.ROOT);
        String paymentUrl = baseUrl + "?" + queryStr + "&vnp_SecureHash=" + secureHash;

        // Lưu txnRef vào order cho callback match
        order.setLastTxnRef(txnRef);
        orderRepository.save(order);

        return paymentUrl;
    }

    /**
     * Tạo URL cho retry: Unique txnRef + lưu lastTxnRef
     */
    @Transactional
    public String createPaymentUrlForRetry(Long orderId, Long licenseId, Account buyer, HttpServletRequest request) {
        CustomerOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order không tồn tại"));

        if (order.getOrderStatus() != CustomerOrder.OrderStatus.PENDING) {
            throw new RuntimeException("Chỉ có thể retry order PENDING!");
        }

        // Cập nhật license/price
        License license = licenseToolRepository.findById(licenseId).orElseThrow();
        order.setLicense(license);
        order.setPrice(license.getPrice() == null ? 0.0 : license.getPrice());
        orderRepository.save(order);

        // Reset transaction PENDING
        WalletTransaction tx = order.getTransaction();
        if (tx == null) {
            tx = new WalletTransaction();
            tx.setWallet(buyer.getWallet());
            tx.setTransactionType(WalletTransaction.TransactionType.BUY);
            tx.setAmount(BigDecimal.valueOf(order.getPrice()));
            tx.setCreatedAt(LocalDateTime.now());
            transactionRepository.save(tx);
            order.setTransaction(tx);
            orderRepository.save(order);
        } else {
            tx.setStatus(WalletTransaction.TransactionStatus.PENDING);
            tx.setAmount(BigDecimal.valueOf(order.getPrice()));
            tx.setCreatedAt(LocalDateTime.now());
            transactionRepository.save(tx);
        }

        // Tạo unique txnRef cho retry (timestamp giống C# Ticks)
        String uniqueTxnRef = String.valueOf(System.currentTimeMillis());

        // OrderInfo cho retry (giống C#, dùng orderId)
        String orderInfo = String.valueOf(orderId);

        long amountInt = Math.round(order.getPrice() * 100);

        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", "2.1.0");
        vnpParams.put("vnp_Command", "pay");
        vnpParams.put("vnp_TmnCode", tmnCode.trim());
        vnpParams.put("vnp_Amount", String.valueOf(amountInt));
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_TxnRef", uniqueTxnRef);
        vnpParams.put("vnp_OrderInfo", orderInfo);
        vnpParams.put("vnp_OrderType", "billpayment");
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", returnUrl.trim());
        vnpParams.put("vnp_IpAddr", request.getRemoteAddr());
        vnpParams.put("vnp_CreateDate", new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));

        // Build hash/query
        String hashDataStr = buildVnpHashData(vnpParams);
        String queryStr = buildVnpQuery(vnpParams);

        String secureHash = hmacSHA512(hashSecret.trim(), hashDataStr).toUpperCase(Locale.ROOT);
        String paymentUrl = baseUrl + "?" + queryStr + "&vnp_SecureHash=" + secureHash;

        // Lưu unique txnRef vào order để callback match
        order.setLastTxnRef(uniqueTxnRef);
        orderRepository.save(order);

        System.out.println("Retry txnRef generated: " + uniqueTxnRef + " for orderId: " + orderId);

        return paymentUrl;
    }

    /**
     * Helper build hash data (không lambda)
     */
    private String buildVnpHashData(Map<String, String> params) {
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        for (String fieldName : fieldNames) {
            String value = params.get(fieldName);
            if (value == null || value.length() == 0) continue;

            if (hashData.length() > 0) {
                hashData.append('&');
            }

            try {
                String encodedValue = URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
                hashData.append(fieldName).append('=').append(encodedValue);
            } catch (Exception e) {
                throw new RuntimeException("Encoding error at field: " + fieldName, e);
            }
        }
        return hashData.toString();
    }

    /**
     * Helper build query string (không lambda)
     */
    private String buildVnpQuery(Map<String, String> params) {
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);

        StringBuilder query = new StringBuilder();
        for (String fieldName : fieldNames) {
            String value = params.get(fieldName);
            if (value == null || value.length() == 0) continue;

            if (query.length() > 0) {
                query.append('&');
            }

            try {
                String encodedValue = URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
                String encodedFieldName = URLEncoder.encode(fieldName, StandardCharsets.UTF_8.toString());
                query.append(encodedFieldName).append('=').append(encodedValue);
            } catch (Exception e) {
                throw new RuntimeException("Encoding error at field: " + fieldName, e);
            }
        }
        return query.toString();
    }

    /**
     * Xử lý kết quả thanh toán từ VNPay (match txnRef với lastTxnRef)
     */
    @Transactional
    public boolean handlePaymentCallback(Map<String, String> params) {
        try {
            String responseCode = params.get("vnp_ResponseCode");
            String txnRef = params.get("vnp_TxnRef");
            String orderInfo = params.get("vnp_OrderInfo");
            double amount = Double.parseDouble(params.get("vnp_Amount")) / 100.0;

            boolean success = "00".equals(responseCode);

            // FIX OPTIONAL: Tìm order bằng lastTxnRef
            Optional<CustomerOrder> optionalOrder = orderRepository.findByLastTxnRef(txnRef);
            CustomerOrder order = optionalOrder.orElseThrow(() -> new RuntimeException("Order không tồn tại với txnRef: " + txnRef));

            System.out.println("Callback matched orderId: " + order.getOrderId() + " with txnRef: " + txnRef);

            WalletTransaction tx = order.getTransaction();
            if (tx == null) throw new RuntimeException("Transaction không liên kết với order!");

            // Update transaction
            tx.setStatus(success ? WalletTransaction.TransactionStatus.SUCCESS : WalletTransaction.TransactionStatus.FAILED);
            tx.setUpdatedAt(LocalDateTime.now());
            transactionRepository.save(tx);

            // Update order
            if (success) {
                order.setOrderStatus(CustomerOrder.OrderStatus.SUCCESS);
                // Cập nhật wallet seller (nhận tiền)
                Account seller = order.getTool().getSeller();
                Wallet sellerWallet = walletRepository.findByAccount(seller).orElseThrow();
                if (sellerWallet.getBalance() == null) sellerWallet.setBalance(BigDecimal.ZERO);
                sellerWallet.setBalance(sellerWallet.getBalance().add(BigDecimal.valueOf(amount)));
                sellerWallet.setUpdatedAt(LocalDateTime.now());
                walletRepository.save(sellerWallet);

                // Giảm quantity tool
                Tool tool = order.getTool();
                if (tool.getQuantity() > 0) {
                    tool.setQuantity(tool.getQuantity() - 1);
                    toolRepository.save(tool);
                }

                // Tạo license account + gửi email
                createAndAssignLicense(order);
            } else {
                // Fail: Giữ PENDING để retry
                order.setOrderStatus(CustomerOrder.OrderStatus.PENDING);
                String message = params.get("vnp_Message") != null ? params.get("vnp_Message") : "Unknown error";
                System.err.println("Payment failed for order " + order.getOrderId() + ": " + message + " (Code: " + responseCode + ")");
            }
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);

            return success;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // REFACTOR: Tách tạo license account (giống code cũ)
    private void createAndAssignLicense(CustomerOrder order) {
        Tool tool = order.getTool();
        License license = order.getLicense();
        Account buyer = order.getAccount();

        LicenseAccount acc = new LicenseAccount();
        acc.setLicense(license);
        acc.setOrder(order);
        acc.setTool(tool);
        acc.setUsed(true);
        acc.setStatus(LicenseAccount.Status.ACTIVE);
        acc.setStartDate(LocalDateTime.now());
        acc.setEndDate(LocalDateTime.now().plusDays(license.getDurationDays()));

        String loginMethod = tool.getLoginMethod().toString();
        acc.setLoginMethod(LicenseAccount.LoginMethod.valueOf(loginMethod));

        if ("USER_PASSWORD".equals(loginMethod)) {
            acc.setUsername("user_" + buyer.getAccountId() + "_" + System.currentTimeMillis());
            acc.setPassword(UUID.randomUUID().toString().substring(0, 8));
            licenseAccountRepository.save(acc);
            sendUserPasswordEmail(order, acc);
        } else if ("TOKEN".equals(loginMethod)) {
            Optional<LicenseAccount> unusedToken = licenseAccountRepository.findFirstByToolAndUsedFalse(tool);
            if (unusedToken.isPresent()) {
                LicenseAccount tokenAcc = unusedToken.get();
                tokenAcc.setUsed(true);
                tokenAcc.setOrder(order);
                tokenAcc.setStatus(LicenseAccount.Status.ACTIVE);
                tokenAcc.setStartDate(LocalDateTime.now());
                tokenAcc.setEndDate(LocalDateTime.now().plusDays(license.getDurationDays()));
                licenseAccountRepository.save(tokenAcc);
                sendTokenEmail(order, tokenAcc);
            }
        }
    }

    /**
     * Gửi mail cho tool dạng USER_PASSWORD
     */
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

        } catch (Exception e) {
            System.err.println("Gửi email USER_PASSWORD thất bại: " + e.getMessage());
        }
    }

    /**
     * Gửi mail cho tool dạng TOKEN
     */
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

        } catch (Exception e) {
            System.err.println("Gửi email TOKEN thất bại: " + e.getMessage());
        }
    }

    /**
     * Sinh mã HMAC SHA512 (VNPay dùng để ký request)
     */
    private String hmacSHA512(String key, String data) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac.init(secretKey);
            byte[] hash = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error generating HMAC", e);
        }
    }
}