package swp391.fa25.lms.service.seller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.SellerPackage;
import swp391.fa25.lms.repository.AccountRepository;
import swp391.fa25.lms.repository.SellerPackageRepository;
import swp391.fa25.lms.service.seller.SellerService;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class PaymentPackageService {

    @Autowired private SellerPackageRepository sellerPackageRepository;
    @Autowired private SellerService sellerService;
    @Autowired private AccountRepository accountRepository;

    @Value("${vnpay.tmnCode}")
    private String tmnCode;
    @Value("${vnpay.hashSecret}")
    private String hashSecret;
    @Value("${vnpay.baseUrl}")
    private String baseUrl;
    @Value("${vnpay.returnUrl}")
    private String returnUrl;

    // Tạo link thanh toán cho Seller
    public String createPaymentUrlForSeller(int packageId, Account account, HttpServletRequest request) {
        SellerPackage pkg = sellerPackageRepository.findById(packageId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy gói Seller"));

        long amountInt = Math.round(pkg.getPrice() * 100);

        Map<String, String> vnpParams = new LinkedHashMap<>();
        vnpParams.put("vnp_Version", "2.1.0");
        vnpParams.put("vnp_Command", "pay");
        vnpParams.put("vnp_TmnCode", tmnCode.trim());
        vnpParams.put("vnp_Amount", String.valueOf(amountInt));
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_TxnRef", String.valueOf(System.currentTimeMillis()));
        vnpParams.put("vnp_OrderInfo", "SELLER_" + packageId + "_" + account.getAccountId());
        vnpParams.put("vnp_OrderType", "billpayment");
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", returnUrl.trim());
        vnpParams.put("vnp_IpAddr", request.getRemoteAddr());
        vnpParams.put("vnp_CreateDate", new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));

        return buildPaymentUrl(vnpParams);
    }

    // Xử lý callback từ VNPay (chỉ cho Seller)
    @Transactional
    public boolean handlePaymentCallback(Map<String, String> params) {
        try {
            String responseCode = params.get("vnp_ResponseCode");
            String orderInfo = params.get("vnp_OrderInfo");
            boolean success = "00".equals(responseCode);

            if (orderInfo.startsWith("SELLER_")) {
                String[] parts = orderInfo.split("_");
                int packageId = Integer.parseInt(parts[1]);
                Long accountId = Long.parseLong(parts[2]);
                Account acc = accountRepository.findById(accountId).orElseThrow();

                if (success) {
                    sellerService.renewSeller(acc.getEmail(), packageId);
                }
            }
            return success;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Helper tạo URL VNPay
    private String buildPaymentUrl(Map<String, String> params) {
        try {
            List<String> fieldNames = new ArrayList<>(params.keySet());
            Collections.sort(fieldNames);
            StringBuilder hashData = new StringBuilder();
            StringBuilder query = new StringBuilder();

            for (String fieldName : fieldNames) {
                String value = params.get(fieldName);
                if (value == null || value.isEmpty()) continue;

                if (hashData.length() > 0) {
                    hashData.append('&');
                    query.append('&');
                }

                String encodedField = URLEncoder.encode(fieldName, StandardCharsets.UTF_8);
                String encodedValue = URLEncoder.encode(value, StandardCharsets.UTF_8);

                hashData.append(fieldName).append('=').append(encodedValue);
                query.append(encodedField).append('=').append(encodedValue);
            }

            String secureHash = hmacSHA512(hashSecret.trim(), hashData.toString()).toUpperCase(Locale.ROOT);
            return baseUrl + "?" + query + "&vnp_SecureHash=" + secureHash;

        } catch (Exception e) {
            throw new RuntimeException("Error building VNPay URL", e);
        }
    }

    // Helper hash HMAC
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
