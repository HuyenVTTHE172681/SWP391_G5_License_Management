package swp391.fa25.lms.controller.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.service.AccountService;

import java.util.Map;


@Controller
public class WebAuthController {
    @Autowired
    private AccountService accountService;

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("account", new Account());
        return "public/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute Account account,
                           BindingResult result,
                           String confirmPassword,
                           RedirectAttributes redirectAttributes,
                           Model model) {
        if (result.hasErrors()) {
            model.addAttribute("showAlert", true);
            model.addAttribute("alertType", "danger");
            model.addAttribute("alertMessage", "Please correct the errors below.");
            return "public/register";
        }

        // Check password confirmation
        if (!account.getPassword().equals(confirmPassword)) {
            result.rejectValue("password", "error.confirmPassword", "Passwords do not match.");
            model.addAttribute("showAlert", true);
            model.addAttribute("alertType", "danger");
            model.addAttribute("alertMessage", "Passwords do not match.");
            return "public/register";
        }

        // Additional client-side validation for phone (since model only has pattern, not notBlank)
        if (account.getPhone() == null || account.getPhone().trim().isEmpty()) {
            result.rejectValue("phone", "error.phone", "Phone number is required.");
            model.addAttribute("showAlert", true);
            model.addAttribute("alertType", "danger");
            model.addAttribute("alertMessage", "Phone number is required.");
            return "public/register";
        }

        try {
            accountService.registerAccount(account);

            //  Gán thông báo
            model.addAttribute("showAlert", true);
            model.addAttribute("alertType", "success");
            model.addAttribute("alertMessage", "Registration successful! Please check your email to verify your account.");

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
            model.addAttribute("message", "Email verification successful! You can now log in.");
            model.addAttribute("redirectUrl", "/login");

        } catch (RuntimeException e) {
            model.addAttribute("verified", false);
            model.addAttribute("message", e.getMessage());
        }
        return "public/verify-result";
    }



}
