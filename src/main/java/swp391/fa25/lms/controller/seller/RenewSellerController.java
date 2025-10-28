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
import swp391.fa25.lms.service.seller.SellerService;

import java.util.List;


@Controller
@RequestMapping("/seller")
public class RenewSellerController {
    @Autowired
    private SellerService sellerService;

    @GetMapping("/renew")
    public String showRenewSeller(Model model){
        model.addAttribute("packages", sellerService.getAllPackage());
        return "seller/renewSeller";
    }

    @PostMapping("/renew")
    public String renewSeller(@RequestParam("packageId") int packageId,
                              Authentication authentication,
                              Model model){
        String email = authentication.getName();
        Account account = sellerService.renewSeller(email, packageId);
        model.addAttribute("message", "Gia hạn thành công đến ngày: " + account.getSellerExpiryDate().toLocalDate());
        model.addAttribute("packages", sellerService.getAllPackage());
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
