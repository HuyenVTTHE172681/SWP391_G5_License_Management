package swp391.fa25.lms.controller.seller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.repository.AccountRepository;
import swp391.fa25.lms.service.seller.PaymentPackageService;

import java.util.Map;

@Controller
@RequestMapping("/seller")
public class PaymentPackageController {

    @Autowired
    private PaymentPackageService paymentPackageService;

    @Autowired
    private AccountRepository accountRepository;

    // 🔄 VNPay callback trả về sau thanh toán Seller
    @GetMapping("/payment-return")
    public String paymentReturn(@RequestParam Map<String, String> params,
                                Map<String, Object> model,
                                HttpServletRequest request) {
        boolean success = paymentPackageService.handlePaymentCallback(params);

        if (success) {

            String orderInfo = params.get("vnp_OrderInfo");
            if (orderInfo != null && orderInfo.startsWith("SELLER_")) {
                String[] parts = orderInfo.split("_");
                Long accountId = Long.parseLong(parts[2]);

                Account updatedAccount = accountRepository.findById(accountId)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản sau khi thanh toán"));

                HttpSession session = request.getSession(false);
                if (session != null) {
                    session.setAttribute("loggedInAccount", updatedAccount);
                }
            }

            model.put("message", "🎉 Thanh toán thành công! Gói Seller đã được kích hoạt.");

            return "seller/paymentSuccess";
        } else {
            model.put("message", "❌ Thanh toán thất bại hoặc bị hủy.");
            return "seller/paymentFailed";
        }
    }
}