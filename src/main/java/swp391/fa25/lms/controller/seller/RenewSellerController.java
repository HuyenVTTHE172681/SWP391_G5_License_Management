package swp391.fa25.lms.controller.seller;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpServletRequest;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.SellerSubscription;
import swp391.fa25.lms.repository.AccountRepository;
import swp391.fa25.lms.service.seller.PaymentPackageService;
import swp391.fa25.lms.service.seller.SellerService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/seller")
public class RenewSellerController {

    @Autowired
    private SellerService sellerService;

    @Autowired
    private AccountRepository accountRepo;

    @Autowired
    private PaymentPackageService paymentService;

    // 🧾 Hiển thị trang chọn gói
    @GetMapping("/renew")
    public String showRenewPage(HttpSession session, Model model) {
        // Lấy account đúng key
        Account account = (Account) session.getAttribute("loggedInAccount");

        // Nếu chưa đăng nhập → chuyển hướng
        if (account == null) {
            return "redirect:/login";
        }

        // Cập nhật thông tin mới nhất từ DB (nếu cần)
        Account freshAcc = accountRepo.findByEmail(account.getEmail()).orElse(null);
        if (freshAcc == null) {
            session.invalidate();
            return "redirect:/login";
        }

        // Kiểm tra hạn seller
        LocalDateTime expiryDate = freshAcc.getSellerExpiryDate();
        if (expiryDate == null) {
            model.addAttribute("warning", "Bạn chưa kích hoạt gói Seller. Vui lòng chọn gói phù hợp!");
        } else if (expiryDate.isBefore(LocalDateTime.now())) {
            model.addAttribute("warning", "Gói Seller của bạn đã hết hạn! Vui lòng gia hạn để tiếp tục.");
        } else {
            model.addAttribute("info", "Gói hiện tại còn hạn đến: " + expiryDate.toLocalDate());
        }

        // Đưa danh sách gói ra view
        model.addAttribute("packages", sellerService.getAllPackage());

        return "seller/renewSeller";


        // Cua em PHUCHUY
//        if (account != null) {
//            String email = account.getName();
//            Account account = accountRepo.findByEmail(email).orElse(null);
//
//            if (account != null) {
//                if (account.getSellerExpiryDate() == null) {
//                    model.addAttribute("warning", "Bạn chưa kích hoạt gói Seller. Vui lòng chọn gói phù hợp!");
//                } else if (account.getSellerExpiryDate().isBefore(LocalDateTime.now())) {
//                    model.addAttribute("warning", "⚠️ Gói Seller của bạn đã hết hạn! Vui lòng gia hạn để tiếp tục.");
//                } else {
//                    model.addAttribute("info", "⏳ Gói hiện tại còn hạn đến: " + account.getSellerExpiryDate().toLocalDate());
//                }
//            }
//        }
//
//        model.addAttribute("packages", sellerService.getAllPackage());
//        return "seller/renewSeller";
    }

    // 💳 Gửi form chọn gói → redirect sang VNPay
    @PostMapping("/renew")
    public String renewSeller(@RequestParam("packageId") int packageId,
                              Authentication authentication,
                              HttpServletRequest request) {
        String email = authentication.getName();
        Account account = accountRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));

        String paymentUrl = paymentService.createPaymentUrlForSeller(packageId, account, request);
        return "redirect:" + paymentUrl;
    }

    // 🔄 VNPay callback trả về sau thanh toán
    @GetMapping("/payment-return")
    public String handlePaymentReturn(@RequestParam Map<String, String> params, Model model) {
        boolean success = paymentService.handlePaymentCallback(params);
        if (success) {
            model.addAttribute("message", "🎉 Thanh toán thành công! Gói Seller đã được kích hoạt.");
            return "seller/paymentSuccess";
        } else {
            model.addAttribute("message", "❌ Thanh toán thất bại hoặc bị hủy.");
            return "seller/paymentFailed";
        }
    }

    // 📜 Xem lịch sử gia hạn
    @GetMapping("/history")
    public String viewHistory(Authentication authentication, Model model) {
        String email = authentication.getName();
        List<SellerSubscription> history = sellerService.getSubscriptionHistory(email);
        model.addAttribute("subscriptions", history);
        return "seller/subscriptionHistory";
    }
}
