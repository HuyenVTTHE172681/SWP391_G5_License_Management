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
     * Nếu đặt lại mật khẩu thành công redirect về login
     */
    @PostMapping("/reset-password")
    public String handleResetPassword(@RequestParam("token") String token,
                                      @RequestParam("newPassword") String newPassword,
                                      @RequestParam("confirmPassword") String confirmPassword,
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

            // Xác định lỗi ở ô nào
            if (message.contains("xác nhận")) {
                redirectAttributes.addFlashAttribute("confirmPasswordError", message);
            } else {
                redirectAttributes.addFlashAttribute("newPasswordError", message);
            }

            redirectAttributes.addFlashAttribute("showAlert", true);
            redirectAttributes.addFlashAttribute("alertType", "danger");
            redirectAttributes.addFlashAttribute("alertMessage", message);

            // Quay lại form
            return "redirect:/reset-password/" + token;
        }
    }

}
