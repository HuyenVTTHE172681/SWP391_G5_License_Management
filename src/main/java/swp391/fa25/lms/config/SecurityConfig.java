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

//    @Bean
//    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//        http
//                .csrf(csrf -> csrf.disable())
//                // Tắt formLogin mặc định
//                .formLogin(form -> form.disable())
//                .authorizeHttpRequests(auth -> auth
//                        // Các đường dẫn public
//                        .requestMatchers(
//                                "/",
//                                "/home",
//                                "/home/**",
//                                "/register",
//                                "/verify-email/**",
//                                "/forgot-password",
//                                "/reset-password/**",
//                                "/css/**",
//                                "/js/**",
//                                "/images/**",
//                                "/assets/**").permitAll()
//                        // Phân quyền theo role
//                        .requestMatchers("/admin/**").hasRole("ADMIN")
//                        .requestMatchers("/seller/**").hasRole("SELLER")
//                        .requestMatchers("/moderator/**").hasRole("MOD")
//                        .requestMatchers("/manager/**").hasRole("MANAGER")
//                        .anyRequest().authenticated()
//                )
//                .logout(logout -> logout
//                        .logoutUrl("/logout")
//                        .logoutSuccessUrl("/logout")
//                        .permitAll()
//                );
//
//        return http.build();
//    }

@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
            // Tắt xác thực CSRF để tránh lỗi POST form
            .csrf(csrf -> csrf.disable())
            .formLogin(form -> form.disable())
            // Cho phép tất cả request không cần đăng nhập
            .authorizeHttpRequests(auth -> auth
                    .anyRequest().permitAll()
            )

            // Tắt hoàn toàn form login và logout
            .formLogin(form -> form.disable())
            .logout(logout -> logout.disable());

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
