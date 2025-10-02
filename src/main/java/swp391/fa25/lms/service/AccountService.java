package swp391.fa25.lms.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.Role;
import swp391.fa25.lms.repository.AccountRepo;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.MimeMessageHelper;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class AccountService {
    private final AccountRepo accountRepo;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.verification.token.expiry:15}") // mặc định 15 phút
    private int tokenExpiryMinutes;

    @Value("${app.base-url:http://localhost:7070}") // mặc định chạy local
    private String baseUrl;

    // Regex kiểm tra format email
    private static final Pattern EMAIL_REGEX = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    public AccountService(AccountRepo accountRepo, JavaMailSender mailSender, PasswordEncoder passwordEncoder) {
        this.accountRepo = accountRepo;
        this.mailSender = mailSender;
        this.passwordEncoder = passwordEncoder;
    }

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
        Role customerRole = new Role();
        customerRole.setRoleId(2);
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

    public String verifyAccount(String token) {
        Account account = accountRepo.findByVerificationToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid token"));

        if (account.getTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token expired");
        }

        account.setVerified(true);
        account.setVerificationToken(null);
        account.setTokenExpiry(null);

        accountRepo.save(account);
        return "Account verified successfully!";
    }
}
