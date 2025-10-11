package swp391.fa25.lms.controller.common;

import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.repository.AccountRepo;
import swp391.fa25.lms.service.AccountService;

@Controller

public class ProfileController {
    @Autowired
    private AccountService accountService;

    @GetMapping("/profile")
    public String viewProfile(Authentication authentication, Model model) {
        String email = authentication.getName(); // lấy email (username) của user đang đăng nhập
        Account account = accountService.viewProfile(email);
        model.addAttribute("account", account);

        switch (account.getRole().getRoleName()) {
            case CUSTOMER:
                return "profile/customerProfile";
            case SELLER:
                return "profile/sellerProfile";
            case MANAGER:
                return "profile/managerProfile";
            case MOD:
                return "profile/modProfile";
            default:
                return "error/unauthorized";
        }
    }


    @GetMapping("/profile/edit")
    public String editProfile(Authentication authentication, Model model) {
        String email = authentication.getName();
        Account account = accountService.viewProfile(email);
        model.addAttribute("account", account);
        switch (account.getRole().getRoleName()) {
            case CUSTOMER:
                return "profile/editProfile";
            case SELLER:
                return "profile/editProfile";
            case MANAGER:
                return "profile/editProfile";
            case MOD:
                return "profile/editProfile";
            default:
                return "error/unauthorized";
        }
    }

    @PostMapping("/profile/edit")
    public String updateProfile(@ModelAttribute("account") Account updatedAccount,
                                Authentication authentication) {
        String email = authentication.getName();
        accountService.updateProfile(email, updatedAccount);
        return "redirect:/profile";
    }
}