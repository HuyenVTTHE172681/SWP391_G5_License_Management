package swp391.fa25.lms.controller.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.service.customer.AccountService;

@Controller

public class ProfileController {
    @Autowired
    private AccountService accountService;

    /**
     * View Profile
     * - TH1: Nếu Spring Security có Authentication -> lấy email từ authentication.getName()
     * - TH2: Nếu không -> fallback lấy account từ session attribute "loggedInAccount"
     * - TH3: Không có -> redirect /login
     */
    @GetMapping("/profile")
    public String viewProfile(Authentication authentication, Model model, HttpServletRequest request) {
        String email =  null;

        // TH1: Authentication
        if(authentication != null && authentication.isAuthenticated()
            && authentication.getPrincipal() != null) {
            email = authentication.getName();
        }

        // TH2: Lấy từ session (nếu controller login lưu session.loggedInAccount)
        if (email == null) {
            Account sessionAcc = (Account) request.getSession().getAttribute("loggedInAccount");
            if (sessionAcc != null) {
                email = sessionAcc.getEmail();
            }
        }

        // TH3: Nếu vẫn null => chưa login => redirect tới login
        if (email == null) {
            return "redirect:/login";
        }

        Account account = accountService.viewProfile(email);
        model.addAttribute("account", account);

        return "profile/customerProfile";
    }


    @GetMapping("/profile/edit")
    public String editProfile(Authentication authentication, Model model, HttpServletRequest request) {
        String email = null;

        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() != null
                && !(authentication.getPrincipal() instanceof String && authentication.getPrincipal().equals("anonymousUser"))) {
            email = authentication.getName();
        }

        if (email == null) {
            Account sessionAccount = (Account) request.getSession().getAttribute("loggedInAccount");
            if (sessionAccount != null) {
                email = sessionAccount.getEmail();
            }
        }

        if (email == null) {
            return "redirect:/login";
        }
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
                                Authentication authentication,
                                HttpServletRequest request,
                                Model model) {
        try {
            String email = authentication != null ? authentication.getName() : null;

            if (email == null) {
                Account sessionAccount = (Account) request.getSession().getAttribute("loggedInAccount");
                if (sessionAccount != null) email = sessionAccount.getEmail();
            }

            if (email == null) return "redirect:/login";

            accountService.updateProfile(email, updatedAccount);
            model.addAttribute("success", "Cập nhật thông tin thành công!");
            return "redirect:/profile";

        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("account", updatedAccount); // Giữ lại dữ liệu đã nhập
            return "profile/editProfile";
        }
    }
}