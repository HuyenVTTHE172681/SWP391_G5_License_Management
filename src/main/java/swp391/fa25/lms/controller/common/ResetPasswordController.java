package swp391.fa25.lms.controller.common;

import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.repository.AccountRepository;

import java.util.Optional;

@Controller
@RequestMapping("/profile")
public class ResetPasswordController {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public ResetPasswordController(AccountRepository accountRepository, PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Hiển thị form đổi mật khẩu
    @GetMapping("/reset-password")
    public String showResetPasswordForm() {
        // vì file HTML nằm trong templates/public/
        return "public/reset-passwordProfile";
    }

    // Xử lý đổi mật khẩu (không cần token)
    @PostMapping("/reset-password")
    public String handleResetPassword(
            @RequestParam("currentPassword") String currentPassword,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("confirmPassword") String confirmPassword,
            Authentication authentication,
            Model model
    ) {
        // Lấy tài khoản hiện tại từ người đang đăng nhập
        Optional<Account> optionalAccount = accountRepository.findByEmail(authentication.getName());

        if (optionalAccount.isEmpty()) {
            model.addAttribute("error", "Không tìm thấy tài khoản.");
            return "public/reset-password";
        }

        Account account = optionalAccount.get();

        // Kiểm tra mật khẩu hiện tại
        if (!passwordEncoder.matches(currentPassword, account.getPassword())) {
            model.addAttribute("error", "Mật khẩu hiện tại không đúng.");
            return "public/reset-passwordProfile";
        }

        // Kiểm tra xác nhận mật khẩu mới
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "Mật khẩu xác nhận không khớp.");
            return "public/reset-passwordProfile";
        }

        // Kiểm tra nếu mật khẩu mới trùng với mật khẩu cũ
        if (passwordEncoder.matches(newPassword, account.getPassword())) {
            model.addAttribute("error", "Mật khẩu mới không được trùng với mật khẩu hiện tại.");
            return "public/reset-passwordProfile";
        }

        // Cập nhật mật khẩu mới
        account.setPassword(passwordEncoder.encode(newPassword));
        accountRepository.save(account);

        model.addAttribute("message", "Đổi mật khẩu thành công!");
        return "public/reset-passwordProfile";
    }
}
