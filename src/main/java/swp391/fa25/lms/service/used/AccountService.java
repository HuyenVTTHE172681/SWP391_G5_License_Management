package swp391.fa25.lms.service.used;

import org.apache.coyote.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.Role;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import swp391.fa25.lms.repository.AccountRepository;
import swp391.fa25.lms.repository.RoleRepository;
import swp391.fa25.lms.util.JwtService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class AccountService {
    private static final Logger logger = LoggerFactory.getLogger(AccountService.class);
    private final AccountRepository accountRepo;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RoleRepository roleRepository;

    @Value("${app.verification.token.expiry:15}") // mặc định 15 phút
    private int tokenExpiryMinutes;

    @Value("${app.base-url:http://localhost:7070}") // mặc định chạy local
    private String baseUrl;

    // Regex kiểm tra format email
    private static final Pattern EMAIL_REGEX = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    public AccountService(AccountRepository accountRepo, PasswordEncoder passwordEncoder,
                          JavaMailSender mailSender, JwtService jwtService, RoleRepository roleRepository) {
        this.accountRepo = accountRepo;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
        this.jwtService = jwtService;
        this.roleRepository = roleRepository;
    }

    /**
     * Đăng ký tài khoản
     * - Validate tất cả fields (không để trống, định dạng, độ dài, ...)
     * - Kiểm tra email chưa được verified trước đó
     * - Mã hóa mật khẩu
     * - Sinh mã xác minh 6 chữ số, set expiry (tokenExpiryMinutes)
     * - Gán default role = CUSTOMER, status = DEACTIVATED, verified = false
     * - Lưu DB và gửi email
     * @param account
     */
    public void registerAccount(Account account) {
        // Trim toàn bộ
        account.setEmail(account.getEmail().trim());
        account.setFullName(account.getFullName().trim());
        account.setPhone(account.getPhone().trim());

        // 1 Validate full name
        if (account.getFullName().isEmpty())
            throw new RuntimeException("Họ và tên không được để trống.");
        if (account.getFullName().length() < 5 || account.getFullName().length() > 20)
            throw new RuntimeException("Họ và tên phải từ 5–20 ký tự.");
        if (!account.getFullName().matches("^[\\p{L}\\s]+$"))
            throw new RuntimeException("Họ và tên chỉ được chứa chữ cái và khoảng trắng.");

        // 2 Validate phone
        if (!account.getPhone().matches("^0[0-9]{9,10}$"))
            throw new RuntimeException("Số điện thoại không hợp lệ. Phải bắt đầu bằng 0 và có tối đa 11 số.");

        // 3 Validate email
        if (account.getEmail().isEmpty())
            throw new RuntimeException("Email không được để trống.");
        if (account.getEmail().contains(" "))
            throw new RuntimeException("Email không được chứa khoảng trắng.");
        if (!account.getEmail().matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))
            throw new RuntimeException("Email không hợp lệ.");
        if (account.getEmail().length() > 50)
            throw new RuntimeException("Email không được vượt quá 50 ký tự.");
        if (accountRepo.existsByEmailAndVerifiedTrue(account.getEmail()))
            throw new RuntimeException("Email này đã được đăng ký.");

        // 4 Validate mật khẩu
        if (account.getPassword() == null || account.getPassword().isEmpty())
            throw new RuntimeException("Mật khẩu không được để trống.");
        if (!account.getPassword().matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*]).{8,}$"))
            throw new RuntimeException("Mật khẩu phải có ít nhất 8 ký tự, bao gồm 1 chữ hoa, 1 chữ thường, 1 số và 1 ký tự đặc biệt.");

        // Mã hóa mật khẩu
        account.setPassword(passwordEncoder.encode(account.getPassword()));

        // Sinh mã xác thực 6 chữ số
        String code = String.format("%06d", new Random().nextInt(999999));
        account.setVerificationCode(code);
        account.setCodeExpiry(LocalDateTime.now().plusMinutes(tokenExpiryMinutes));

        // Cac fields default
        account.setStatus(Account.AccountStatus.DEACTIVATED);
        account.setVerified(false);
        account.setCreatedAt(LocalDateTime.now());
        Role role = roleRepository.findByRoleName(Role.RoleName.CUSTOMER)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy role CUSTOMER"));
        account.setRole(role);

        // Luu Db
        accountRepo.save(account);

        // Gui email
        sendVerificationCode(account, code);
    }

    // Gửi mã xác minh sau khi dang ky thanh cong
    private void sendVerificationCode(Account account, String code) {
        try {
            String subject = "[LMS] Xác minh tài khoản";
            String body = "<p>Xin chào <b>" + account.getFullName() + "</b>,</p>"
                    + "<p>Mã xác minh của bạn là: <b>" + code + "</b></p>"
                    + "<p>Mã này có hiệu lực trong " + tokenExpiryMinutes + " phút.</p>";

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(account.getEmail());
            helper.setSubject(subject);
            helper.setText(body, true);

            mailSender.send(message);

        } catch (Exception e) {
            throw new RuntimeException("Không thể gửi email xác minh. Vui lòng thử lại sau.");
        }
    }

    // Verify Code
    public void verifyCode( String code) {
        Account acc = accountRepo.findByVerificationCode(code)
                .orElseThrow(() -> new RuntimeException("Email không tồn tại."));

        if (Boolean.TRUE.equals(acc.getVerified()))
            throw new RuntimeException("Tài khoản đã được xác minh trước đó.");

        if (acc.getCodeExpiry().isBefore(LocalDateTime.now()))
            throw new RuntimeException("Mã xác thực đã hết hạn.");

        if (acc.getVerificationCode() == null || !acc.getVerificationCode().equals(code))
            throw new RuntimeException("Mã xác thực không đúng.");

        // Cập nhật trạng thái
        acc.setVerified(true);
        acc.setVerificationCode(null);
        acc.setCodeExpiry(null);
        acc.setStatus(Account.AccountStatus.ACTIVE);
        acc.setUpdatedAt(LocalDateTime.now());

        accountRepo.save(acc);
    }

    /**
     * Xác thực tài khoản cho LOGIN
     * - Validate input (email, password)
     * - Check email tồn tại
     * - Check password
     * - Check trạng thái tài khoản (verified, active)
     *
     * @param email email người dùng nhập
     * @param password
     * @return
     */
    public Account login(String email, String password) {

        // 1 Validate input email
        if(email == null || email.trim().isEmpty()) {
            throw new RuntimeException("Email không được để trống");
        }
        if (!EMAIL_REGEX.matcher(email).matches()) {
            throw new RuntimeException("Định dạng email không hợp lệ");
        }
        Account account = accountRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email không tồn tại trong hệ thống"));

        // 2 Validate input password
        if (password == null || password.trim().isEmpty()) {
            throw new RuntimeException("Mật khẩu không được để trống");
        }
        if (!passwordEncoder.matches(password, account.getPassword())) {
            throw new RuntimeException("Mật khẩu không đúng");
        }

        //  3 Kiểm tra trạng thái tài khoản
        if (account.getStatus() == Account.AccountStatus.DEACTIVATED) {
            throw new RuntimeException("Tài khoản đã bị vô hiệu hóa");
        }
        if (!Boolean.TRUE.equals(account.getVerified())) {
            throw new RuntimeException("Tài khoản chưa xác minh email");
        }

//        Map<String, String> tokens = new HashMap<>();
//        tokens.put("accessToken", jwtService.generateAccessToken(account));
//        tokens.put("refreshToken", jwtService.generateRefreshToken(account));

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

    // View Profile
    public Account viewProfile(String email){
        return accountRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản email: " + email));
    }


    public Account updateProfile(String email, Account updatedAccount) {
        Account existing = accountRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản email: " + email));

        existing.setFullName(updatedAccount.getFullName());
        existing.setPhone(updatedAccount.getPhone());
        existing.setAddress(updatedAccount.getAddress());

        return accountRepo.save(existing);
    }

    public void sendResetPasswordEmail(String email) {
        Account account = accountRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email not found"));

        // Tạo token reset
        String token = UUID.randomUUID().toString();
        account.setVerificationToken(token);
        account.setTokenExpiry(LocalDateTime.now().plusMinutes(tokenExpiryMinutes));
        accountRepo.save(account);

        // Gửi mail reset
        try {
            String subject = "[LMS] Reset Your Password";
            String resetUrl = baseUrl + "/reset-password?token=" + token;

            String body = "<p>Hello <b>" + account.getFullName() + "</b>,</p>"
                    + "<p>We received a request to reset your password. Click below to reset it:</p>"
                    + "<p><a href=\"" + resetUrl + "\" style=\"display:inline-block;padding:10px 15px;"
                    + "background-color:#007bff;color:#fff;text-decoration:none;border-radius:5px;\">"
                    + "Reset Password</a></p>"
                    + "<p>This link will expire in " + tokenExpiryMinutes + " minutes.</p>";

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(email);
            helper.setSubject(subject);
            helper.setText(body, true);
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send reset password email");
        }
    }

    public void resetPassword(String token, String newPassword) {
        Account account = accountRepo.findByVerificationToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid or expired token"));

        if (account.getTokenExpiry() == null || account.getTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Reset link has expired");
        }

        // Cập nhật mật khẩu
        account.setPassword(passwordEncoder.encode(newPassword));
        account.setVerificationToken(null);
        account.setTokenExpiry(null);
        accountRepo.save(account);
    }

}
