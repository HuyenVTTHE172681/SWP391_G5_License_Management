package swp391.fa25.lms.service.used;

import org.apache.coyote.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.Role;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import swp391.fa25.lms.repository.AccountRepository;
import swp391.fa25.lms.repository.RoleRepository;
import swp391.fa25.lms.util.JwtService;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class AccountService {
    private static final Logger logger = LoggerFactory.getLogger(AccountService.class);
    private final AccountRepository accountRepo;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RoleRepository roleRepository;

    @Value("2") // mặc định 15 phút
    private int tokenExpiryMinutes;

    @Value("${app.base-url:http://localhost:7070}") // mặc định chạy local
    private String baseUrl;

    // Regex kiểm tra format email
    private static final Pattern EMAIL_REGEX = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private static final Pattern PASS_REGEX = Pattern.compile("" +
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*]).{8,}$"
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
     * ====================== Đăng ký tài khoản
     * - Validate tất cả fields (không để trống, định dạng, độ dài, ...)
     * - Kiểm tra email chưa được verified trước đó
     * - Mã hóa mật khẩu
     * - Sinh mã xác minh 6 chữ số, set expiry (tokenExpiryMinutes)
     * - Gán default role = CUSTOMER, status = DEACTIVATED, verified = false
     * - Lưu DB và gửi email
     * @param account
     */
    public boolean registerAccount(Account account,  BindingResult result) {
        // Trim toàn bộ
        account.setEmail(account.getEmail().trim());
        account.setFullName(account.getFullName().trim());
        account.setPhone(account.getPhone().trim());

        // Kiểm tra email đã tồn tại chưa
        Optional<Account> existingOpt = accountRepo.findByEmail(account.getEmail());

        if (existingOpt.isPresent()) {
            Account existing = existingOpt.get();

            // Nếu đã ACTIVE -> không cho đăng ký lại
            if (existing.getStatus() == Account.AccountStatus.ACTIVE && Boolean.TRUE.equals(existing.getVerified())) {
                result.rejectValue("email", "error.email", "Email này đã được đăng ký và kích hoạt.");
                return false;
            }

            // Nếu DEACTIVE -> xóa tài khoản cũ để cho phép đăng ký lại
            if (existing.getStatus() == Account.AccountStatus.DEACTIVATED || !existing.getVerified()) {
                accountRepo.delete(existing); // Xóa record cũ (vì chưa xác minh)
            }
        }

        //  Validate mật khẩu
        if (account.getPassword().contains(" ")) {
            result.rejectValue("password", "error.password", "Mật khẩu không được chứa khoảng trắng.");
        } else if (!account.getPassword()
                .matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*]).{8,}$")) {
            result.rejectValue("password", "error.password",
                    "Mật khẩu phải có ít nhất 8 ký tự, bao gồm 1 chữ hoa, 1 chữ thường, 1 số và 1 ký tự đặc biệt.");
        }

        // Nếu có lỗi validate thì dừng lại
        if (result.hasErrors()) {
            return false;
        }

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

        return true;
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

    // Verify Code sau khi dang ky thanh cong
    public void verifyCode( String code) {
        Account acc = accountRepo.findByVerificationCode(code)
                .orElseThrow(() -> new RuntimeException("Mã xác thực không đúng."));

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
     * ====================== Xác thực tài khoản cho LOGIN
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

    /**
     * ====================== Đặt lại mật khẩu RESET PASSWORD
     *
     */

    public void resetPassword(String token, String newPassword, String confirmPassword) {
        Account account = accountRepo.findByVerificationToken(token)
                .orElseThrow(() -> new RuntimeException("Link không hợp lệ hoặc đã hết hạn."));

        // Check token có hết hạn không
        if (account.getTokenExpiry().isBefore(LocalDateTime.now()) || account.getTokenExpiry() == null) {
            account.setVerificationToken(null);
            account.setTokenExpiry(null);
            accountRepo.save(account);

            throw new RuntimeException("Token đã hết hạn, vui lòng yêu cầu lại đặt lại mật khẩu.");
        }

        //  Validate mật khẩu
        if (!newPassword.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*]).{8,}$")) {
            throw new RuntimeException("Mật khẩu phải có ít nhất 8 ký tự, bao gồm 1 chữ hoa, 1 chữ thường, 1 số và 1 ký tự đặc biệt.");
        } else if(newPassword.contains(" ")) {
            throw new RuntimeException("Mật khẩu không được chứa khoảng trắng.");
        }


        // Check confirmPassword
        if (!newPassword.equals(confirmPassword)) {
            throw new RuntimeException("Mật khẩu xác nhận không khớp.");
        } else if (confirmPassword == null || confirmPassword.isBlank()) {
            throw new RuntimeException("Vui lòng xác nhận mật khẩu.");
        }

        // Cập nhật mật khẩu
        account.setPassword(passwordEncoder.encode(newPassword));
        account.setVerificationToken(null);
        account.setTokenExpiry(null);

        accountRepo.save(account);
    }

    // Kiểm tra token hợp lệ (tồn tại và chưa hết hạn)
    public boolean isValidResetToken(String token) {
        return accountRepo.findByVerificationToken(token)
                .filter(acc -> acc.getTokenExpiry() != null && acc.getTokenExpiry().isAfter(LocalDateTime.now()))
                .isPresent();
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
            String subject = "[LMS] Đặt lại mật khẩu";
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
