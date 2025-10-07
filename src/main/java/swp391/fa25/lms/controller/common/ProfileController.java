package swp391.fa25.lms.controller.common;

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
    public String viewProfile(HttpSession session, Model model) {
        Account loggedInUser = (Account) session.getAttribute("loggedInUser");
        if(loggedInUser == null){
            return "redirect:/login";
        }
        Account account = accountService.getAccountById(loggedInUser.getAccountId());
        model.addAttribute("account", account);
        switch (account.getRole().getRoleName()) {
            case CUSTOMER:
                return "public/customer-profile";
            case SELLER:
                return "seller/seller-profile";
            case MOD:
                return "moderator/moderator-profile";
            case MANAGER:
                return "manager/manager-profile";
            default:
                return "error/403"; // nếu không có quyền
        }
    }

    @GetMapping("/profile/edit")
    public String editProfile(HttpSession session, Model model) {
        Account loggedInUser = (Account) session.getAttribute("loggedInUser");
        if(loggedInUser == null){
            return "redirect:/login";
        }
        Account account = accountService.getAccountById(loggedInUser.getAccountId());
        model.addAttribute("account", account);
        return "profile/edit-profile";
    }

    @PostMapping("/profile/edit")
    public String updateProfile(@ModelAttribute("account") Account updatedAccount,
                                HttpSession session, Model model) {
        Account loggedInUser = (Account) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }

        // Giữ nguyên role, password, status... (nếu không cho chỉnh)
        updatedAccount.setAccountId(loggedInUser.getAccountId());
        updatedAccount.setRole(loggedInUser.getRole());
        updatedAccount.setStatus(loggedInUser.getStatus());
        updatedAccount.setPassword(loggedInUser.getPassword());

        accountService.updateAccount(updatedAccount);

        // Cập nhật session
        session.setAttribute("loggedInUser", updatedAccount);

        model.addAttribute("message", "Cập nhật hồ sơ thành công!");
        return "redirect:/profile";
    }
}