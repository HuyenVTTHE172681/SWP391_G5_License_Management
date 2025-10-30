package swp391.fa25.lms.controller.seller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.SellerSubscription;
import swp391.fa25.lms.repository.AccountRepository;
import swp391.fa25.lms.service.customer.AccountService;
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

    @GetMapping("/renew")
    public String showRenewPage(Authentication authentication, Model model) {
        if (authentication != null) {
            String email = authentication.getName();
            Account account = accountRepo.findByEmail(email).orElse(null);
            if (account != null) {
                if (account.getSellerExpiryDate() == null) {
                    model.addAttribute("warning", "Bạn chưa kích hoạt gói Seller. Vui lòng chọn gói phù hợp!");
                } else if (account.getSellerExpiryDate().isBefore(LocalDateTime.now())) {
                    model.addAttribute("warning", "Gói Seller của bạn đã hết hạn! Vui lòng gia hạn để tiếp tục.");
                } else {
                    model.addAttribute("info", "Gói hiện tại của bạn còn hạn đến: " + account.getSellerExpiryDate().toLocalDate());
                }
            }
        }

        model.addAttribute("packages", sellerService.getAllPackage());
        return "seller/renewSeller";
    }

    @PostMapping("/renew")
    public String renewSeller(@RequestParam("packageId") int packageId,
                              Authentication authentication,
                              Model model) {
        String email = authentication.getName();
        Account account = sellerService.renewSeller(email, packageId);

        model.addAttribute("message", "🎉 Gia hạn thành công! Hiệu lực đến ngày: " + account.getSellerExpiryDate().toLocalDate());
        model.addAttribute("packages", sellerService.getAllPackage());
        model.addAttribute("info", "Gói hiện tại còn hạn đến: " + account.getSellerExpiryDate().toLocalDate());
        return "seller/renewSeller";
    }

    @GetMapping("/history")
    public String viewHistory(Authentication authentication, Model model) {
        String email = authentication.getName();
        List<SellerSubscription> history = sellerService.getSubscriptionHistory(email);
        model.addAttribute("subscriptions", history);
        return "seller/subscriptionHistory";
    }
}
