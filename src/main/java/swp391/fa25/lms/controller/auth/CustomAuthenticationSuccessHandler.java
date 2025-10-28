package swp391.fa25.lms.controller.auth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.repository.AccountRepository;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private AccountRepository accountRepo;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        String email = authentication.getName();
        Account account = accountRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản!"));

        // ✅ Nếu role là SELLER
        if (account.getRole() != null && account.getRole().getRoleName().name().equals("SELLER")) {

            // ❌ Nếu chưa gia hạn
            if (account.getSellerPackage() == null || !Boolean.TRUE.equals(account.getSellerActive())) {
                response.sendRedirect("/seller/renew");
                return;
            }

            // ❌ Nếu đã hết hạn
            if (account.getSellerExpiryDate() == null ||
                    account.getSellerExpiryDate().isBefore(LocalDateTime.now())) {

                account.setSellerActive(false);
                accountRepo.save(account);
                response.sendRedirect("/seller/renew");
                return;
            }

            // ✅ Nếu hợp lệ
            response.sendRedirect("/seller/tools");
            return;
        }

        // ✅ Nếu là user bình thường
        response.sendRedirect("/home");
    }
}
