package swp391.fa25.lms.controller.auth;

import jakarta.servlet.http.HttpServletRequest;
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
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.service.AccountService;
import java.util.List;

@Controller
public class WebAuthController {
    public static final Logger logger = LoggerFactory.getLogger(WebAuthController.class);

    @Autowired
    private AccountService accountService;

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        if (!model.containsAttribute("account")) {
            model.addAttribute("account", new Account());
        }
        return "public/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("account") Account account,
                           BindingResult result,
                           @RequestParam("confirmPassword") String confirmPassword,
                           RedirectAttributes redirectAttributes,
                           Model model) {
        // Check password confirmation
        if (!account.getPassword().equals(confirmPassword)) {
            result.rejectValue("password", "error.password", "Mật khẩu không khớp.");
        }

        // Check lỗi validation từ model
        if (result.hasErrors()) {
            model.addAttribute("showAlert", true);
            model.addAttribute("alertType", "danger");
            model.addAttribute("alertMessage", "Vui lòng sửa các lỗi bên dưới và thử lại.");
            return "public/register";
        }

        // Additional client-side validation for phone (since model only has pattern, not notBlank)
        if (account.getPhone() == null || account.getPhone().trim().isEmpty()) {
            result.rejectValue("phone", "error.phone", "Bắt buộc phải nhập số điện thoại");
            model.addAttribute("showAlert", true);
            model.addAttribute("alertType", "danger");
            model.addAttribute("alertMessage", "Bắt buộc phải nhập số điện thoại.");
            return "public/register";
        }

        try {
            accountService.registerAccount(account);
            //  Gán thông báo
            model.addAttribute("showAlert", true);
            model.addAttribute("alertType", "success");
            model.addAttribute("alertMessage", "Đăng ký thành công! Vui lòng kiểm tra email để xác minh tài khoản của bạn.");

            // Reset account trống
            model.addAttribute("account", new Account());

            return "public/register";
        } catch (RuntimeException e) {
            model.addAttribute("showAlert", true);
            model.addAttribute("alertType", "danger");
            model.addAttribute("alertMessage", e.getMessage());
            return "public/register";
        }
    }

    @GetMapping("/verify-email/{token}")
    public String verifyEmail(@PathVariable("token") String token, Model model) {
        try {
            Account account = accountService.verifyAccount(token);

            model.addAttribute("verified", true);
            model.addAttribute("message", "Xác minh email thành công! Bây giờ bạn có thể đăng nhập.");
            model.addAttribute("redirectUrl", "/login");

        } catch (RuntimeException e) {
            model.addAttribute("verified", false);
            model.addAttribute("message", e.getMessage());
        }
        return "public/verify-result";
    }

    // Hien thi form login
    @GetMapping("/login")
    public String showLoginForm(Model model) {
        if (!model.containsAttribute("email")) {
            model.addAttribute("email", "");
        }
        return "public/login";
    }

    @PostMapping("/login")
    public String doLogin(@RequestParam String email,
                          @RequestParam String password,
                          HttpServletRequest request,
                          RedirectAttributes redirectAttributes) {
        logger.info("Login attempt for email={}", email);
        logger.info("Login attempt for email={} password={}", email, password);

        try {
            // Check -> trả về Account
            Account account = accountService.loginForWeb(email, password);

            // Hiển thị password phiên bản "masked".
            request.getSession().setAttribute("loggedInAccount", account);
            request.getSession().setAttribute("maskedPassword", "********");

            // Redirect theo role
            String redirect;
            if (account.getRole() != null && account.getRole().getRoleName() != null) {
                switch (account.getRole().getRoleName()) {
                    case CUSTOMER:
                        redirect = "redirect:/home";
                        break;
                    case SELLER:
                        redirect = "redirect:/seller/dashboard";
                        break;
                    case MOD:
                        redirect = "redirect:/mod/dashboard";
                        break;
                    default:
                        redirect = "redirect:/home";
                }
            } else {
                redirect = "redirect:/home";
            }

            logger.info("Login success for email={}, redirect={}", email, redirect);
            return redirect;

        } catch (RuntimeException ex) {
            // Gửi message về form login (sử dụng flash để giữ message qua redirect)
            redirectAttributes.addFlashAttribute("showAlert", true);
            redirectAttributes.addFlashAttribute("alertType", "danger");
            redirectAttributes.addFlashAttribute("alertMessage", ex.getMessage());
            redirectAttributes.addFlashAttribute("email", email);
            logger.warn("Login failed for email={} : {}", email, ex.getMessage());
            return "redirect:/login";
        }
    }


}
