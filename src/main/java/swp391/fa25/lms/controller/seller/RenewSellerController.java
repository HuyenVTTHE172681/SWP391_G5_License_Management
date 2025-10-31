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

@Controller
@RequestMapping("/seller")
public class RenewSellerController {

    @Autowired
    private SellerService sellerService;

    @Autowired
    private AccountRepository accountRepo;

    @Autowired
    private PaymentPackageService paymentPackageService;

    // 🧾 Hiển thị trang chọn gói
    @GetMapping("/renew")
    public String showRenewPage(HttpSession session, Model model) {
        Account account = (Account) session.getAttribute("loggedInAccount");
        if (account != null) {
            if (account.getSellerExpiryDate() == null) {
                model.addAttribute("warning", "Bạn chưa kích hoạt gói Seller. Vui lòng chọn gói phù hợp!");
            } else if (account.getSellerExpiryDate().isBefore(LocalDateTime.now())) {
                model.addAttribute("warning", "⚠️ Gói Seller của bạn đã hết hạn! Vui lòng gia hạn để tiếp tục.");
            } else {
                model.addAttribute("info", "⏳ Gói hiện tại còn hạn đến: " + account.getSellerExpiryDate().toLocalDate());
            }
        }
        model.addAttribute("packages", sellerService.getAllPackage());
        return "seller/renewSeller";
    }

    // 💳 Gửi form chọn gói → redirect sang VNPay
    @PostMapping("/renew")
    public String renewSeller(@RequestParam("packageId") int packageId,
                              Authentication authentication,
                              HttpServletRequest request) {
        String email = authentication.getName();
        Account account = accountRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));

        // Dùng PaymentPackageService (service dành riêng cho Seller)
        String paymentUrl = paymentPackageService.createPaymentUrlForSeller(packageId, account, request);
        return "redirect:" + paymentUrl;
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
