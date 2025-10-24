package swp391.fa25.lms.controller.auth;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.service.customer.AccountService;

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
                                 Model model) {

        //  Lỗi xác nhận mật khẩu
        if (!account.getPassword().equals(account.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "error.confirmPassword", "Mật khẩu không khớp.");
        }

        boolean success = accountService.registerAccount(account, result);

        //  Nếu có lỗi ở lại trang đăng ký
        if (result.hasErrors() || !success) {
            model.addAttribute("showAlert", true);
            model.addAttribute("alertType", "danger");
            model.addAttribute("alertMessage", "Vui lòng sửa các lỗi bên dưới và thử lại.");
            return "public/register";
        }

        // Nếu thành công hiển thị alert rồi chuyển sang verify
        model.addAttribute("showAlert", true);
        model.addAttribute("alertType", "success");
        model.addAttribute("alertMessage", "Đăng ký thành công! Vui lòng kiểm tra email để lấy mã xác minh.");
        model.addAttribute("redirectUrl", "/verify");
        return "public/register";

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
            model.addAttribute("successMessage", "Xác minh thành công! Bạn có thể đăng nhập ngay.");
            model.addAttribute("redirectUrl", "/login");
        } catch (RuntimeException e) {
            model.addAttribute("errorMessage", e.getMessage());
        }
        return "public/verify-code";
    }

}
