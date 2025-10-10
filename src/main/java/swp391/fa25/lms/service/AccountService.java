package swp391.fa25.lms.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.Role;
import swp391.fa25.lms.repository.*;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import swp391.fa25.lms.repository.RoleRepo;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class AccountService {
    private static final Logger logger = LoggerFactory.getLogger(AccountService.class);
    private final AccountRepo accountRepo;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RoleRepo roleRepo;

    @Value("${app.verification.token.expiry:15}") // mặc định 15 phút
    private int tokenExpiryMinutes;

    @Value("${app.base-url:http://localhost:7070}") // mặc định chạy local
    private String baseUrl;

    // Regex kiểm tra format email
    private static final Pattern EMAIL_REGEX = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    public AccountService(AccountRepo accountRepo, PasswordEncoder passwordEncoder,
                          JavaMailSender mailSender, JwtService jwtService, RoleRepo roleRepo) {
        this.accountRepo = accountRepo;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
        this.jwtService = jwtService;
        this.roleRepo = roleRepo;
    }

    // Register account
    public Account registerAccount(Account account) {
        // 1. Check format email
        if (account.getEmail() == null || !EMAIL_REGEX.matcher(account.getEmail()).matches()) {
            throw new RuntimeException("Invalid email format!");
        }
        // 2. Check email đã tồn tại trong DB
        if (accountRepo.findByEmail(account.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists!");
        }
        // 3. Encode password
        account.setPassword(passwordEncoder.encode(account.getPassword()));

        account.setCreatedAt(LocalDateTime.now());
        account.setUpdatedAt(LocalDateTime.now());
        account.setStatus(Account.AccountStatus.ACTIVE);
        Role customerRole = roleRepo.findByRoleName(Role.RoleName.CUSTOMER)
                .orElseThrow(() -> new RuntimeException("Default role CUSTOMER not found"));
        account.setRole(customerRole);
        account.setRole(customerRole);

        // 4. Tạo token verify
        String token = UUID.randomUUID().toString();
        account.setVerificationToken(token);
        account.setTokenExpiry(LocalDateTime.now().plusMinutes(tokenExpiryMinutes));
        account.setVerified(false);

        Account saved = accountRepo.save(account);

        // 5. Gửi email xác thực
        sendVerificationEmail(saved);

        return saved;
    }

    // Send email verification account after register success
    private void sendVerificationEmail(Account account) {
        try {
            String subject = "[LMS - Register New Account] Please Verify Your Email";
            String verifyUrl = baseUrl + "/verify-email/" + account.getVerificationToken();

            String body = "<p>Hello <b>" + account.getFullName() + "</b>,</p>"
                    + "<p>Please verify your email address by clicking the link below:</p>"
                    + "<p><a href=\"" + verifyUrl + "\" style=\"display:inline-block;padding:10px 15px;"
                    + "background-color:#4CAF50;color:#fff;text-decoration:none;border-radius:5px;\">"
                    + "Verify Email</a></p>"
                    + "<p>This link will expire in " + tokenExpiryMinutes + " minutes.</p>";

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(account.getEmail());
            helper.setSubject(subject);
            helper.setText(body, true);

            mailSender.send(message);

        } catch (Exception e) {
            throw new RuntimeException("Failed to send verification email");
        }
    }

    // Verify account
    public Account verifyAccount(String token) {
        Account account = accountRepo.findByVerificationToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid token"));

        if (account.getTokenExpiry() == null || account.getTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token confirmation has expired. Please request a new verification email.");
        }

        account.setVerified(true);
        account.setVerificationToken(null);
        account.setTokenExpiry(null);

        accountRepo.save(account);
        return account;
    }

    // Login
    public Account loginForWeb(String email, String password) {
        Account account = accountRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email not found"));

        if (!passwordEncoder.matches(password, account.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        if (!account.getVerified()) {
            throw new RuntimeException("Account not verified");
        }

        if (account.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new RuntimeException("Account not ACTIVE");
        }

        return account;
    }

    // Generate token reset password and send email
    public void generateResetPasswordToken(String email) {
        Account account = accountRepo.findByEmail(email) .orElseThrow(() -> new RuntimeException("Email không tồn tại trong hệ thống."));

        String token = UUID.randomUUID().toString();
        account.setVerificationToken(token);
        account.setTokenExpiry(LocalDateTime.now().plusMinutes(tokenExpiryMinutes));

        accountRepo.save(account);
        sendResetPasswordEmail(account);
    }

    // Send email reset password
    public void sendResetPasswordEmail(Account account) {
        try {
            String subject = "[LMS] Reset Your Password";
            String resetUrl = baseUrl + "/reset-password/" + account.getVerificationToken();

            String body = "<p>Xin chào <b>" + account.getFullName() + "</b>,</p>"
                    + "<p>Bạn đã yêu cầu đặt lại mật khẩu. Hãy nhấn vào liên kết bên dưới:</p>"
                    + "<p><a href=\"" + resetUrl + "\" style=\"display:inline-block;padding:10px 15px;"
                    + "background-color:#007bff;color:#fff;text-decoration:none;border-radius:5px;\">"
                    + "Đặt lại mật khẩu</a></p>"
                    + "<p>Liên kết sẽ hết hạn trong " + tokenExpiryMinutes + " phút.</p>";

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(account.getEmail());
            helper.setSubject(subject);
            helper.setText(body, true);

            mailSender.send(message);
        } catch (Exception e) {
            logger.error("Send reset email failed", e);
            throw new RuntimeException("Không thể gửi email đặt lại mật khẩu.");
        }
    }

    // Reset password
    public void resetPassword(String token, String newPassword, String confirmPassword) {
        Account account = accountRepo.findByVerificationToken(token)
                .orElseThrow(() -> new RuntimeException("Token không hợp lệ hoặc đã hết hạn."));

        if (account.getTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token đã hết hạn, vui lòng yêu cầu lại.");
        }

        // Validate password
        StringBuilder error = new StringBuilder();
        if(newPassword.length() < 8) error.append("Mật khẩu phải có ít nhất 8 ký tự. ");
        if (!newPassword.matches(".*[A-Z].*")) error.append("Phải có ít nhất 1 chữ in hoa. ");
        if (!newPassword.matches(".*[a-z].*")) error.append("Phải có ít nhất 1 chữ thường. ");
        if (!newPassword.matches(".*\\d.*")) error.append("Phải có ít nhất 1 số. ");
        if (!newPassword.matches(".*[!@#$%^&*(),.?\":{}|<>].*")) error.append("Phải có ít nhất 1 ký tự đặc biệt. ");

        if (error.length() > 0) throw new RuntimeException(error.toString().trim());

        if (!newPassword.equals(confirmPassword)) {
            throw new RuntimeException("Mật khẩu xác nhận không khớp.");
        }

        // Encode và lưu
        account.setPassword(passwordEncoder.encode(newPassword));
        account.setVerificationToken(null);
        account.setTokenExpiry(null);

        accountRepo.save(account);
    }

}
