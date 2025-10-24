package swp391.fa25.lms.service.customer;

import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class PaymentService {
    private ToolRepository toolRepository;
    private LicenseToolRepository licenseToolRepository;
    private CustomerOrderRepository orderRepository;
    private WalletTransactionRepository transactionRepository;
    private LicenseAccountRepository licenseAccountRepository;
    private JavaMailSender mailSender;
    private WalletRepository walletRepository;

    @Value("${vnpay.tmnCode}")
    private String tmnCode;
    @Value("${vnpay.hashSecret}")
    private String hashSecret;
    @Value("${vnpay.baseUrl}")
    private String baseUrl;
    @Value("${vnpay.returnUrl}")
    private String returnUrl;

    public PaymentService(ToolRepository toolRepository, LicenseToolRepository licenseToolRepository, CustomerOrderRepository orderRepository, WalletTransactionRepository transactionRepository, LicenseAccountRepository licenseAccountRepository, JavaMailSender mailSender, WalletRepository walletRepository) {
        this.toolRepository = toolRepository;
        this.licenseToolRepository = licenseToolRepository;
        this.orderRepository = orderRepository;
        this.transactionRepository = transactionRepository;
        this.licenseAccountRepository = licenseAccountRepository;
        this.mailSender = mailSender;
        this.walletRepository = walletRepository;
    }

    /**
     * Tạo URL thanh toán VNPay và redirect user sang đó
     */
    public String createPaymentUrl(Long toolId, Long licenseId, Account buyer, HttpServletRequest request) {
        Tool tool = toolRepository.findById(toolId).orElseThrow();
        License license = licenseToolRepository.findById(licenseId).orElseThrow();

        // VNPay yêu cầu amount * 100 (đơn vị: VND x 100)
        long amountInt = Math.round((license.getPrice() == null ? 0.0 : license.getPrice()) * 100);

        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", "2.1.0");
        vnpParams.put("vnp_Command", "pay");
        vnpParams.put("vnp_TmnCode", tmnCode.trim());
        vnpParams.put("vnp_Amount", String.valueOf(amountInt));
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_TxnRef", String.valueOf(System.currentTimeMillis()));
        vnpParams.put("vnp_OrderInfo", toolId + "_" + licenseId + "_" + buyer.getAccountId());
        vnpParams.put("vnp_OrderType", "billpayment");
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", returnUrl.trim());
        vnpParams.put("vnp_IpAddr", request.getRemoteAddr());
        vnpParams.put("vnp_CreateDate", new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));

        // Sắp xếp key tăng dần để tạo chuỗi hash chính xác
        List<String> fieldNames = new ArrayList<>(vnpParams.keySet());
        Collections.sort(fieldNames);

        // Tạo chuỗi dữ liệu để hash và query string để redirect
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        for (String fieldName : fieldNames) {
            String value = vnpParams.get(fieldName);
            if (value == null || value.length() == 0) continue;

            if (hashData.length() > 0) {
                hashData.append('&');
                query.append('&');
            }

            try {
                // Encode an toàn, UTF-8
                String encodedValue = URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
                String encodedFieldName = URLEncoder.encode(fieldName, StandardCharsets.UTF_8.toString());

                // build hashData và query
                hashData.append(fieldName).append('=').append(encodedValue);
                query.append(encodedFieldName).append('=').append(encodedValue);
            } catch (Exception e) {
                throw new RuntimeException("Encoding error at field: " + fieldName, e);
            }
        }

        // Tạo chữ ký bảo mật (HMAC SHA512)
        String secureHash = hmacSHA512(hashSecret.trim(), hashData.toString()).toUpperCase(Locale.ROOT);

        // URL thanh toán hoàn chỉnh
        String paymentUrl = baseUrl + "?" + query.toString() + "&vnp_SecureHash=" + secureHash;
        return paymentUrl;
    }

    /**
     * Xử lý callback trả về từ VNPay (sau khi thanh toán)
     */
    @Transactional
    public boolean handlePaymentCallback(Map<String, String> params) {
        try {
            String responseCode = params.get("vnp_ResponseCode");
            String orderInfo = params.get("vnp_OrderInfo");
            double amount = Double.parseDouble(params.get("vnp_Amount")) / 100.0;

            boolean success = "00".equals(responseCode);

            // Parse: toolId_licenseId_accountId
            String[] parts = orderInfo.split("_");
            Long toolId = Long.parseLong(parts[0]);
            Long licenseId = Long.parseLong(parts[1]);
            Long buyerId = Long.parseLong(parts[2]);

            Tool tool = toolRepository.findById(toolId).orElseThrow();
            License license = licenseToolRepository.findById(licenseId).orElseThrow();

            // Lấy seller của tool (ví seller sẽ nhận tiền)
            Account seller = tool.getSeller();
            if (seller == null) throw new RuntimeException("Tool has no seller linked!");

            Wallet wallet = walletRepository.findByAccount(seller)
                    .orElseThrow(() -> new RuntimeException("Wallet not found for seller ID: " + seller.getAccountId()));

            // 1 Ghi nhận giao dịch vào bảng WalletTransaction
            WalletTransaction tx = new WalletTransaction();
            tx.setWallet(wallet);
            tx.setTransactionType(WalletTransaction.TransactionType.BUY);
            tx.setStatus(success ? WalletTransaction.TransactionStatus.SUCCESS : WalletTransaction.TransactionStatus.FAILED);
            tx.setAmount(BigDecimal.valueOf(amount));
            tx.setCreatedAt(LocalDateTime.now());
            transactionRepository.save(tx);

            // 2 Cập nhật số dư ví nếu thanh toán thành công
            if (success) {
                if (wallet.getBalance() == null) wallet.setBalance(BigDecimal.ZERO);
                wallet.setBalance(wallet.getBalance().add(BigDecimal.valueOf(amount)));
                wallet.setUpdatedAt(LocalDateTime.now());
                walletRepository.save(wallet);
            }

            // 3 Tạo mới đơn hàng
            Account buyer = new Account();
            buyer.setAccountId(buyerId);

            CustomerOrder order = new CustomerOrder();
            order.setAccount(buyer);
            order.setTool(tool);
            order.setLicense(license);
            order.setPrice(amount);
            order.setPaymentMethod(CustomerOrder.PaymentMethod.BANK);
            order.setTransaction(tx);
            order.setCreatedAt(LocalDateTime.now());
            order.setOrderStatus(success ? CustomerOrder.OrderStatus.SUCCESS : CustomerOrder.OrderStatus.FAILED);
            orderRepository.save(order);

            // 4 Nếu thanh toán thành công → tạo license + gửi email
            if (success) {
                LicenseAccount acc = new LicenseAccount();
                acc.setUsername("user_" + buyerId);
                acc.setPassword(UUID.randomUUID().toString().substring(0, 8));
                acc.setLicense(license);
                acc.setOrder(order);
                acc.setTool(tool);
                acc.setStatus(LicenseAccount.Status.ACTIVE);
                acc.setStartDate(LocalDateTime.now());
                acc.setEndDate(LocalDateTime.now().plusDays(license.getDurationDays()));
                licenseAccountRepository.save(acc);

                sendOrderSuccessEmail(order, acc);
            }

            return success;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    /**
     * Gửi email xác nhận thanh toán thành công
     */
    private void sendOrderSuccessEmail(CustomerOrder order, LicenseAccount licenseAcc) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String to = order.getAccount().getEmail();
            String subject = "[LMS] Thanh toán thành công!";
            String body = """
                    <h2>Cảm ơn bạn đã mua tool trên Tool Market!</h2>
                    <p><b>Tool:</b> %s</p>
                    <p><b>License:</b> %s</p>
                    <p><b>Giá:</b> %,.0f VND</p>
                    <p><b>Tài khoản sử dụng:</b> %s</p>
                    <p><b>Mật khẩu:</b> %s</p>
                    <p>Hạn sử dụng đến: %s</p>
                    <br/>
                    <p>Chúc bạn có trải nghiệm tuyệt vời!</p>
                    """.formatted(
                    order.getTool().getToolName(),
                    order.getLicense().getName(),
                    order.getPrice(),
                    licenseAcc.getUsername(),
                    licenseAcc.getPassword(),
                    licenseAcc.getEndDate().toLocalDate()
            );

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true);
            mailSender.send(message);

        } catch (Exception e) {
            System.err.println("Gửi email thất bại: " + e.getMessage());
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
