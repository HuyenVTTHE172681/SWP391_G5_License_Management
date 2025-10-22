package swp391.fa25.lms.controller.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.Role;
import swp391.fa25.lms.repository.AccountRepository;
import swp391.fa25.lms.repository.RoleRepository;
import swp391.fa25.lms.service.used.AccountService;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Controller
public class RegisterSellerController {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RoleRepository roleRepo;

    // ✅ Đăng ký seller cho user đã login
    @GetMapping("/registerSeller")
    public String registerSeller(Authentication authentication, Model model) {
        String email = authentication.getName();
        try {
            Account updatedAccount = accountService.registerSeller(email);
            model.addAttribute("updatedAccount", updatedAccount);
            model.addAttribute("message", "Đăng ký seller thành công!");
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
        }
        return "/public/registerSeller";
    }

    // ✅ Hiển thị form đăng ký seller cho guest
    @GetMapping("/registerGuestToSeller")
    public String showRegisterForm() {
        return "/public/registerGuestToSeller";
    }

    // ✅ Xử lý form đăng ký seller cho guest
    @PostMapping("/registerGuestToSeller")
    public String registerGuestToSeller(@RequestParam String fullName,
                                        @RequestParam String email,
                                        @RequestParam String phone,
                                        @RequestParam String address,
                                        @RequestParam String password,
                                        Model model) {

        Optional<Account> existingOpt = accountRepo.findByEmail(email);
        Account account = existingOpt.orElse(new Account());

        // 🔹 Nếu tài khoản đã verified thì chặn
        if (existingOpt.isPresent() && Boolean.TRUE.equals(existingOpt.get().getVerified())) {
            model.addAttribute("error", "Email đã được sử dụng!");
            return "/public/registerGuestToSeller";
        }

        // 🔹 Tạo mã OTP
        String otp = String.format("%06d", new Random().nextInt(999999));

        // 🔹 Gán thông tin
        account.setFullName(fullName);
        account.setEmail(email);
        account.setPhone(phone);
        account.setAddress(address);
        account.setPassword(passwordEncoder.encode(password));
        account.setVerificationCode(otp);
        account.setCodeExpiry(LocalDateTime.now().plusMinutes(10));
        account.setVerified(false);
        account.setStatus(Account.AccountStatus.DEACTIVATED);

        // ✅ Luôn gán role SELLER (dù là account cũ hay mới)
        account.setRole(roleRepo.findByRoleName(Role.RoleName.SELLER)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy role SELLER")));

        accountRepo.saveAndFlush(account);

        // 🔹 Gửi OTP qua email
        accountService.sendVerificationCode(account, otp);

        // 🔹 Sang trang verify-code
        model.addAttribute("email", email);
        return "/public/verify-code-seller";
    }

    // ✅ Xử lý xác minh mã OTP
    @PostMapping("/verifyCode")
    public String verifyCode(@RequestParam String email,
                             @RequestParam String code,
                             Model model) {

        Optional<Account> optionalAccount = accountRepo.findByEmail(email);

        if (optionalAccount.isEmpty()) {
            model.addAttribute("errorMessage", "Không tìm thấy tài khoản!");
            return "/public/verify-code-seller";
        }

        Account account = optionalAccount.get();

        // 🔹 Kiểm tra mã và thời hạn
        if (account.getVerificationCode() == null || !account.getVerificationCode().equals(code)) {
            model.addAttribute("errorMessage", "Mã xác minh không đúng!");
            model.addAttribute("email", email);
            return "/public/verify-code-seller";
        }

        if (account.getCodeExpiry().isBefore(LocalDateTime.now())) {
            model.addAttribute("errorMessage", "Mã xác minh đã hết hạn!");
            model.addAttribute("email", email);
            return "/public/verify-code-seller";
        }

        // ✅ Thành công
        account.setVerified(true);
        account.setVerificationCode(null);
        account.setCodeExpiry(null);
        account.setStatus(Account.AccountStatus.ACTIVE);

        // 🔹 Đảm bảo role SELLER tồn tại
        if (account.getRole() == null) {
            account.setRole(roleRepo.findByRoleName(Role.RoleName.SELLER)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy role SELLER")));
        }

        accountRepo.save(account);

        model.addAttribute("successMessage", "Xác minh thành công! Bạn có thể đăng nhập.");
        return "/public/login";
    }
}
