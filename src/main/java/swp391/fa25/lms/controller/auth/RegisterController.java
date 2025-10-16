package swp391.fa25.lms.controller.auth;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.service.used.AccountService;

@Controller
public class RegisterController {

    @Autowired
    private AccountService accountService;

    // Hiển thị form đăng ký
    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        if (!model.containsAttribute("account")) {
            model.addAttribute("account", new Account());
        }
        return "public/register";
    }

    // Xử lý đăng ký
    @PostMapping("/register")
    public String handleRegister(@Valid @ModelAttribute("account") Account account,
                                 BindingResult result,
                                 @RequestParam("confirmPassword") String confirmPassword,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {

        //  Lỗi xác nhận mật khẩu
        if (!account.getPassword().equals(confirmPassword)) {
            result.rejectValue("password", "error.password", "Mật khẩu không khớp.");
        }

        //  Lỗi binding cơ bản
        if (result.hasErrors()) {
            model.addAttribute("showAlert", true);
            model.addAttribute("alertType", "danger");
            model.addAttribute("alertMessage", "Vui lòng sửa các lỗi bên dưới và thử lại.");
            return "public/register";
        }

        try {
            accountService.registerAccount(account);

            model.addAttribute("showAlert", true);
            model.addAttribute("alertType", "success");
            model.addAttribute("alertMessage", "Đăng ký thành công! Vui lòng kiểm tra email mã xác minh đã được gửi tới email của bạn.");

            // Redirect toi page verify code
            return "redirect:/verify";

        } catch (RuntimeException e) {
            model.addAttribute("showAlert", true);
            model.addAttribute("alertType", "danger");
            model.addAttribute("alertMessage", e.getMessage());
            return "public/register";
        }
    }

    // Hiển thị form nhập mã code
    @GetMapping("/verify")
    public String showVerifyPage(Model model) {
        return "public/verify-code";
    }

    // Xử lý mã code
    @PostMapping("/verify")
    public String verifyCode(@RequestParam("code") String code,
                             Model model) {
        try {
            accountService.verifyCode(code);
            model.addAttribute("verified", true);
            model.addAttribute("message", "Xác minh thành công! Bạn có thể đăng nhập ngay.");

            return "public/login";
        } catch (RuntimeException e) {
            model.addAttribute("verified", false);
            model.addAttribute("message", e.getMessage());
        }
        return "public/verify-code";
    }

}
