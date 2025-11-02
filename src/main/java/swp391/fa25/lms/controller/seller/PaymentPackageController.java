package swp391.fa25.lms.controller.seller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import swp391.fa25.lms.service.seller.PaymentPackageService;

import java.util.Map;

@Controller
@RequestMapping("/seller")
public class PaymentPackageController {

    @Autowired
    private PaymentPackageService paymentPackageService;

    // 🔄 VNPay callback trả về sau thanh toán Seller
    @GetMapping("/payment-return")
    public String paymentReturn(@RequestParam Map<String, String> params,
                                Map<String, Object> model) {
        boolean success = paymentPackageService.handlePaymentCallback(params);
        if (success) {
            model.put("message", "🎉 Thanh toán thành công! Gói Seller đã được kích hoạt.");
            return "seller/paymentSuccess";
        } else {
            model.put("message", "❌ Thanh toán thất bại hoặc bị hủy.");
            return "seller/paymentFailed";
        }
    }
}
