package swp391.fa25.lms.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import jakarta.servlet.http.HttpServletResponse;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.repository.AccountRepo;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    @Bean
    public UserDetailsService userDetailsService(AccountRepo accountRepo) {
        return (String email) -> {
            Account acc = accountRepo.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

            String roleName = (acc.getRole() != null)
                    ? acc.getRole().getRoleName().name()
                    : "CUSTOMER";

            var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + roleName));

            boolean enabled = acc.getStatus() == Account.AccountStatus.ACTIVE
                    && Boolean.TRUE.equals(acc.getVerified());

            return new User(
                    acc.getEmail(),
                    acc.getPassword(),
                    enabled,
                    true,
                    true,
                    true,
                    authorities
            );
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/",
                                "/home",
                                "/home/**",
                                "/login",
                                "/register",
                                "/verify-email/**",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/feedback/**").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login").permitAll()
                        .loginProcessingUrl("/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .successHandler((req, res, auth) -> {
                            var roles = auth.getAuthorities().stream()
                                    .map(a -> a.getAuthority()).collect(java.util.stream.Collectors.toSet());
                            if (roles.contains("ROLE_ADMIN")) res.sendRedirect("/admin/accounts");
                            else if (roles.contains("ROLE_SELLER")) res.sendRedirect("/seller/dashboard");
                            else res.sendRedirect("/home");
                        })
                        .failureUrl("/login?error")
                )
                .logout(lo -> lo.logoutUrl("/logout").logoutSuccessUrl("/login"));
        return http.build();
    }

    private void writeJsonError(HttpServletResponse response, String message, int status) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(status);

        Map<String, Object> error = new HashMap<>();
        error.put("error", message);
        error.put("status", status);

        new ObjectMapper().writeValue(response.getOutputStream(), error);
    }
}
