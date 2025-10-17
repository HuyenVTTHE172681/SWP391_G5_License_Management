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

    // Xử lý gen token cho form đặt lại mật khẩu
    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam("email") String email,
                                        RedirectAttributes redirectAttributes) {
        try {
            accountService.generateResetPasswordToken(email);
            redirectAttributes.addFlashAttribute("showAlert", true);
            redirectAttributes.addFlashAttribute("alertType", "success");
            redirectAttributes.addFlashAttribute("alertMessage", "Một email khôi phục mật khẩu đã được gửi đến " + email);
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("showAlert", true);
            redirectAttributes.addFlashAttribute("alertType", "danger");
            redirectAttributes.addFlashAttribute("alertMessage", e.getMessage());
        }
        return "redirect:/forgot-password";
    }

    /**
     * Hiển thị form đặt lại mật khẩu
     * Nếu token hết hạn hoặc sai redirect về /forgot-password
     */
    @GetMapping("/reset-password/{token}")
    public String showResetPasswordForm(@PathVariable("token") String token,
                                        Model model,
                                        RedirectAttributes redirectAttributes) {

        if (!accountService.isValidResetToken(token)) {
            redirectAttributes.addFlashAttribute("showAlert", true);
            redirectAttributes.addFlashAttribute("alertType", "danger");
            redirectAttributes.addFlashAttribute("alertMessage",
                    "Liên kết đặt lại mật khẩu đã hết hạn hoặc không hợp lệ. Vui lòng yêu cầu lại.");
            return "redirect:/forgot-password";
        }

        model.addAttribute("token", token);
        model.addAttribute("newPasswordError", "");
        model.addAttribute("confirmPasswordError", "");
        return "public/reset-password";
    }

    /**
     * Xử lý đặt lại mật khẩu
     * Nếu lỗi validate hiển thị thông báo lỗi redirect :/reset-password/" + token
     * Nếu đang đặt lại mật khẩu token hết hạn redirect về forgot-password
     * Nếu đặt lại mật khẩu thành công redirect về login
     */
    @PostMapping("/reset-password")
    public String handleResetPassword(@RequestParam("token") String token,
                                      @RequestParam("newPassword") String newPassword,
                                      @RequestParam("confirmPassword") String confirmPassword,
                                      Model model,
                                      RedirectAttributes redirectAttributes) {
        try {
            accountService.resetPassword(token, newPassword, confirmPassword);

            // Thành công
            redirectAttributes.addFlashAttribute("showAlert", true);
            redirectAttributes.addFlashAttribute("alertType", "success");
            redirectAttributes.addFlashAttribute("alertMessage",
                    "Đặt lại mật khẩu thành công! Hãy đăng nhập lại.");
            return "redirect:/login";

        } catch (RuntimeException e) {
            String message = e.getMessage();

            // Token hết hạn trong quá trình đặt lại mật khẩu
            if (message.contains("Token đã hết hạn")) {
                redirectAttributes.addFlashAttribute("showAlert", true);
                redirectAttributes.addFlashAttribute("alertType", "danger");
                redirectAttributes.addFlashAttribute("alertMessage",
                        "Liên kết đặt lại mật khẩu đã hết hạn. Vui lòng yêu cầu lại.");
                return "redirect:/forgot-password";
            }

            // Lỗi validate
            if (message.contains("xác nhận")) {
                model.addAttribute("confirmPasswordError", message);
                model.addAttribute("newPasswordError", "");
            } else {
                model.addAttribute("newPasswordError", message);
                model.addAttribute("confirmPasswordError", "");
            }

            model.addAttribute("token", token);
            model.addAttribute("showAlert", true);
            model.addAttribute("alertType", "danger");
            model.addAttribute("alertMessage", "Vui lòng sửa các lỗi bên dưới và thử lại.");

            return "public/reset-password";
        }
    }

}
