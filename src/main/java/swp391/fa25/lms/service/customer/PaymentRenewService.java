package swp391.fa25.lms.service.customer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import swp391.fa25.lms.model.*;
import swp391.fa25.lms.repository.*;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Service
public class PaymentRenewService {

    private final CustomerOrderRepository orderRepo;
    private final LicenseToolRepository licenseRepo;
    private final WalletRepository walletRepo;
    private final WalletTransactionRepository txRepo;
    private final ToolRepository toolRepo;
    private final AccountRepository accountRepo;
    private final MyToolService myToolService;

    // VNPay configs (renew)
    @Value("${vnpay.tmnCode}")   private String tmnCode;
    @Value("${vnpay.hashSecret}") private String hashSecret;
    @Value("${vnpay.baseUrl}")    private String baseUrl;
    // returnUrl riêng cho renew
    @Value("${vnpay.returnUrlRenew}") private String returnUrlRenew;

    public PaymentRenewService(CustomerOrderRepository orderRepo,
                               LicenseToolRepository licenseRepo,
                               WalletRepository walletRepo,
                               WalletTransactionRepository txRepo,
                               ToolRepository toolRepo,
                               AccountRepository accountRepo,
                               MyToolService myToolService) {
        this.orderRepo = orderRepo;
        this.licenseRepo = licenseRepo;
        this.walletRepo = walletRepo;
        this.txRepo = txRepo;
        this.toolRepo = toolRepo;
        this.accountRepo = accountRepo;
        this.myToolService = myToolService;
    }

    /** Tạo URL VNPay cho gia hạn */
    public String createRenewPaymentUrl(Long orderId, Long licenseId, Account buyer, HttpServletRequest request) {
        CustomerOrder order = orderRepo.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));
        if (!Objects.equals(order.getAccount().getAccountId(), buyer.getAccountId())) {
            throw new IllegalStateException("Bạn không có quyền với đơn hàng này.");
        }
        if (order.getOrderStatus() != CustomerOrder.OrderStatus.SUCCESS) {
            throw new IllegalStateException("Đơn hàng chưa ở trạng thái SUCCESS.");
        }

        License lic = licenseRepo.findById(licenseId)
                .orElseThrow(() -> new IllegalArgumentException("Gói license không hợp lệ."));
        if (!Objects.equals(lic.getTool().getToolId(), order.getTool().getToolId())) {
            throw new IllegalStateException("Gói license không thuộc cùng Tool với đơn hàng.");
        }

        long amountVnp = Math.round((lic.getPrice() == null ? 0.0 : lic.getPrice()) * 100);

        Map<String, String> params = new HashMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", tmnCode.trim());
        params.put("vnp_Amount", String.valueOf(amountVnp));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", String.valueOf(System.currentTimeMillis()));
        // OrderInfo cho renew: RENEW_orderId_licenseId_buyerId
        params.put("vnp_OrderInfo", String.format("RENEW_%d_%d_%d", orderId, licenseId, buyer.getAccountId()));
        params.put("vnp_OrderType", "billpayment");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", returnUrlRenew.trim());
        params.put("vnp_IpAddr", request.getRemoteAddr());
        params.put("vnp_CreateDate", new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));

        String qs = buildQueryAndSign(params);
        return baseUrl + "?" + qs;
    }

    /** VNPay return cho renew */
    @Transactional
    public boolean handleRenewCallback(Map<String, String> params) {
        try {
            String responseCode = params.get("vnp_ResponseCode"); // "00" = success
            String orderInfo    = params.get("vnp_OrderInfo");
            double amount       = Double.parseDouble(params.get("vnp_Amount")) / 100.0;

            if (orderInfo == null || !orderInfo.startsWith("RENEW_")) return false;

            // RENEW_orderId_licenseId_buyerId
            String[] parts = orderInfo.split("_");
            if (parts.length != 4) return false;

            Long orderId   = Long.parseLong(parts[1]);
            Long licenseId = Long.parseLong(parts[2]);
            Long buyerId   = Long.parseLong(parts[3]);

            CustomerOrder order = orderRepo.findByOrderId(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));
            License lic = licenseRepo.findById(licenseId)
                    .orElseThrow(() -> new IllegalArgumentException("License không hợp lệ"));
            Account buyer = accountRepo.findById(buyerId)
                    .orElseThrow(() -> new IllegalArgumentException("Buyer không hợp lệ"));

            if (!Objects.equals(order.getAccount().getAccountId(), buyer.getAccountId())) return false;
            if (order.getOrderStatus() != CustomerOrder.OrderStatus.SUCCESS) return false;

            Tool tool = order.getTool();
            Account seller = tool.getSeller();
            if (seller == null) throw new IllegalStateException("Tool chưa gắn seller");
            Wallet sellerWallet = walletRepo.findByAccount(seller)
                    .orElseThrow(() -> new IllegalStateException("Seller chưa có ví"));

            boolean success = "00".equals(responseCode);

            // Ghi transaction RENEW
            WalletTransaction tx = new WalletTransaction();
            tx.setWallet(sellerWallet);
            tx.setTransactionType(WalletTransaction.TransactionType.RENEW);
            tx.setStatus(success ? WalletTransaction.TransactionStatus.SUCCESS : WalletTransaction.TransactionStatus.FAILED);
            tx.setAmount(BigDecimal.valueOf(amount));
            tx.setCreatedAt(LocalDateTime.now());
            txRepo.save(tx);

            // Cộng ví nếu thành công
            if (success) {
                if (sellerWallet.getBalance() == null) sellerWallet.setBalance(BigDecimal.ZERO);
                sellerWallet.setBalance(sellerWallet.getBalance().add(BigDecimal.valueOf(amount)));
                sellerWallet.setUpdatedAt(LocalDateTime.now());
                walletRepo.save(sellerWallet);

                // Gia hạn + ghi LicenseRenewLog, gắn transaction
                myToolService
                        .renewWithTransaction(orderId, licenseId, tx);
            }

            return success;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    // ===== helpers =====
    private String buildQueryAndSign(Map<String, String> params) {
        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        for (String k : keys) {
            String v = params.get(k);
            if (v == null || v.isEmpty()) continue;

            if (hashData.length() > 0) {
                hashData.append('&');
                query.append('&');
            }

            String ev = url(v);
            String ek = url(k);
            hashData.append(k).append('=').append(ev);
            query.append(ek).append('=').append(ev);
        }

        String secureHash = hmacSHA512(hashSecret.trim(), hashData.toString()).toUpperCase(Locale.ROOT);
        query.append("&vnp_SecureHash=").append(secureHash);
        return query.toString();
    }

    private String url(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private String hmacSHA512(String key, String data) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac.init(secretKey);
            byte[] hash = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error generating HMAC", e);
        }
    }

}
