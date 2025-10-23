package swp391.fa25.lms.controller.auth;

import org.springframework.ui.Model;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import swp391.fa25.lms.service.used.AccountService;

@Controller
public class ForgotPasswordController {
    @Autowired
    private AccountService accountService;

    /**
     * Hiển thị form forgot password để nhập email
     * @return
     */
    @GetMapping("/forgot-password")
    public String showForgotPasswordForm() {
        return "public/forgot-password";
    }

    // Người dùng nhập email → sinh mật khẩu mới → gửi mail
    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam("email") String email,
                                        RedirectAttributes redirectAttributes) {
        try {
            accountService.resetPasswordAndSendMail(email);
            redirectAttributes.addFlashAttribute("showAlert", true);
            redirectAttributes.addFlashAttribute("alertType", "success");
            redirectAttributes.addFlashAttribute("alertMessage",
                    "Mật khẩu mới đã được gửi tới email: " + email);
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("showAlert", true);
            redirectAttributes.addFlashAttribute("alertType", "danger");
            redirectAttributes.addFlashAttribute("alertMessage", e.getMessage());
        }

        return "redirect:/forgot-password";
    }

}
