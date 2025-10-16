package swp391.fa25.lms.controller.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import swp391.fa25.lms.config.CustomerUserDetail;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.service.used.AccountService;

import java.util.List;

@Controller
public class WebAuthController {
    public static final Logger logger = LoggerFactory.getLogger(WebAuthController.class);

    @Autowired
    private AccountService accountService;

    // Hiển thị form login
    @GetMapping("/login")
    public String showLoginForm(Model model,  @RequestParam(value = "error", required = false) String error) {
        // Model attributes để view show lỗi
        if (!model.containsAttribute("email")) model.addAttribute("email", "");
        if (!model.containsAttribute("emailError")) model.addAttribute("emailError", "");
        if (!model.containsAttribute("passwordError")) model.addAttribute("passwordError", "");
        if (error != null) model.addAttribute("alertMessage", "Lỗi đăng nhập");

        return "public/login";
    }

    /**
     * Xử lý POST /login
     * AccountService: Handle validate + kiểm tra DB + mật khẩu
     */
    @PostMapping("/login")
    public String doLogin(@RequestParam String email,
                          @RequestParam String password,
                          HttpServletRequest request,
                          RedirectAttributes redirectAttributes,
                          Model model) {
        try {
            // Gọi service để check — nếu có lỗi, service sẽ ném exception cụ thể
            Account account = accountService.login(email, password);

            // Xác thực thành công, tạo UserDetails và set Authentication vào SecurityContext
            CustomerUserDetail userDetails = new CustomerUserDetail(account);
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);

            // Lưu account vào session nếu cần dùng ở view
            HttpSession session = request.getSession(true);
            session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());
            session.setAttribute("loggedInAccount", account);

            // Redirect theo role
            String roleName = account.getRole() != null ? account.getRole().getRoleName().name() : "CUSTOMER";
            return switch (roleName) {
                case "ADMIN" -> "redirect:/admin/accounts";
                case "SELLER" -> "redirect:/seller/tools";
                case "MOD" -> "redirect:/moderator/";
                case "MANAGER" -> "redirect:/manager/dashboard";
                default -> "redirect:/home";
            };
        } catch (RuntimeException ex) {
            String msg = ex.getMessage() != null ? ex.getMessage() : "Có lỗi xảy ra. Vui lòng thử lại.";
            model.addAttribute("email", email);

            // Hiển thị lỗi tương ứng với input
            if (msg.toLowerCase().contains("email") || msg.toLowerCase().contains("xác minh") || msg.toLowerCase().contains("vô hiệu")) {
                model.addAttribute("emailError", msg);
                model.addAttribute("passwordError", "");
            } else if (msg.toLowerCase().contains("mật khẩu")) {
                model.addAttribute("emailError", "");
                model.addAttribute("passwordError", msg);
            }  else {
                // Lỗi hệ thống khác
                model.addAttribute("emailError", "Lỗi: " + msg);
                model.addAttribute("passwordError", "");
            }
            return "public/login";
        }
    }

    // Enter email to reset password
    @GetMapping("/forgot-password")
    public String showForgotPasswordForm() {
        return "public/forgot-password";
    }

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

    // Reset password
    @GetMapping("/reset-password/{token}")
    public String showResetPasswordForm(@PathVariable("token") String token, Model model) {
        model.addAttribute("token", token);
        return "public/reset-password";
    }

    @PostMapping("/reset-password")
    public String handleResetPassword(@RequestParam("token") String token,
                                      @RequestParam("newPassword") String newPassword,
                                      @RequestParam("confirmPassword") String confirmPassword,
                                      RedirectAttributes redirectAttributes) {
        try {
            accountService.resetPassword(token, newPassword, confirmPassword);
            redirectAttributes.addFlashAttribute("showAlert", true);
            redirectAttributes.addFlashAttribute("alertType", "success");
            redirectAttributes.addFlashAttribute("alertMessage", "Đặt lại mật khẩu thành công! Hãy đăng nhập lại.");
            return "redirect:/login";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("showAlert", true);
            redirectAttributes.addFlashAttribute("alertType", "danger");
            redirectAttributes.addFlashAttribute("alertMessage", e.getMessage());
            return "redirect:/reset-password/" + token;
        }
    }
}
