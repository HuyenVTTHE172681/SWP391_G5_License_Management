package swp391.fa25.lms.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.service.used.CustomUserDetailsService;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class SecurityConfig {

    private CustomUserDetailsService customUserDetailsService;

    public SecurityConfig(CustomUserDetailsService customUserDetailsService) {
        this.customUserDetailsService = customUserDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/home",
                                "/home/**",
                                "/register",
                                "/verify-email/**",
                                "/forgot-password",
                                "/reset-password/**",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/assets/**").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/seller/**").hasRole("SELLER")
                        .requestMatchers("/mod/**").hasRole("MOD")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .successHandler((req, res, auth) -> {
                            CustomerUserDetail userDetails = (CustomerUserDetail) auth.getPrincipal();
                            Account account = userDetails.getAccount();
                            req.getSession().setAttribute("loggedInAccount", account);

                            String role = account.getRole().getRoleName().name();
                            switch (role) {
                                case "ADMIN" -> res.sendRedirect("/admin/accounts");
                                case "SELLER" -> res.sendRedirect("/seller/tools");
                                case "MOD" -> res.sendRedirect("/mod/dashboard");
                                default -> res.sendRedirect("/home");
                            }
                        })
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                );

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
