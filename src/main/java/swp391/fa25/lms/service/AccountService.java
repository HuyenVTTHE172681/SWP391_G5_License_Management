package swp391.fa25.lms.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.repository.AccountRepo;

import java.time.LocalDateTime;
import java.util.UUID;

public class AccountService {
    private AccountRepo accountRepo;
    private JavaMailSender mailSender;
    private PasswordEncoder passwordEncoder;

    public Account registerAccount(Account account) {
        if (accountRepo.findByEmail(account.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        account.setPassword(passwordEncoder.encode(account.getPassword()));

        String token = UUID.randomUUID().toString();
        account.setVerificationToken(token);
        account.setTokenExpiry(LocalDateTime.now().plusMinutes(15));
        account.setVerified(false);

        Account saved = accountRepo.save(account);
        // Send mail verify
        sendVerificationEmail(saved);

        return saved;
    }

    private void sendVerificationEmail(Account account) {
        String subject = "Pleased Verification Your Email";
        String verifyUrl = "http://localhost:8080/verify?token=" + account.getVerificationToken();

        String body = "Hello " + account.getFullName() +
            ",\n\nPlease verify your email address by clicking the link below:\n" +
                "<a href=\"" + verifyUrl + "\">" + verifyUrl + "</a>" +
                "\n\nThis link will expire in 15 minutes.";

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(account.getEmail());
        message.setSubject(subject);
        message.setText(body);

        mailSender.send(message);
    }

    private String verifyEmail(String token) {
        Account account = accountRepo.findByVerificationToken(token).orElseThrow(() -> new RuntimeException("Account not found"));

        if(account.getTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token is expired");
        }

        account.setVerified(true);
        account.setVerificationToken(null);
        account.setTokenExpiry(null);

        accountRepo.save(account);
        return "Account verified successfully";
    }


}

