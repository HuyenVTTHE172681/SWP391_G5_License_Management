package swp391.fa25.lms.controller.common;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.service.used.AccountService;

@Controller
public class RegisterSellerController {

    @Autowired
    private AccountService accountService;

    @GetMapping("/registerSeller")
    public String registerSeller(Authentication authentication, Model model) {
        String email = authentication.getName();
        try {
            Account updatedAccount = accountService.registerSeller(email);
            model.addAttribute("updatedAccount", updatedAccount);
            model.addAttribute("message", "Đăng ký thành công");
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
        }
        return "/public/registerSeller";
    }
}
