package swp391.fa25.lms.controller.common;

import org.springframework.ui.Model;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.repository.AccountRepo;
import swp391.fa25.lms.service.AccountService;

@Controller

public class ProfileController {
    @Autowired
    private AccountService accountService;

    @GetMapping("/profile")
    public String viewProfile(HttpSession session, Model model) {
        Account loggedInUser = (Account) session.getAttribute("loggedInUser");
        if(loggedInUser == null){
            return "redirect:/login";
        }
        Account account = accountService.getAccountById(loggedInUser.getAccountId());
        model.addAttribute("account", account);
        switch (account.getRole().getRoleName()) {
            case CUSTOMER:
                return "profile/customer-profile";
            case SELLER:
                return "profile/seller-profile";
            case MOD:
                return "profile/moderator-profile";
            case MANAGER:
                return "profile/manager-profile";
            default:
                return "error/403"; // nếu không có quyền
        }
    }
}