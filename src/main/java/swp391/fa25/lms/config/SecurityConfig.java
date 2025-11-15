package swp391.fa25.lms.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import swp391.fa25.lms.controller.auth.CustomAuthenticationSuccessHandler;
import swp391.fa25.lms.service.customer.CustomUserDetailsService;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableScheduling
public class SecurityConfig {

    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private CustomAuthenticationSuccessHandler successHandler;

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
                // Tắt formLogin mặc định
                .formLogin(form -> form.disable())
                .authorizeHttpRequests(auth -> auth
                        // Các đường dẫn public không cần đăng nhập
                        .requestMatchers(
                                "/",
                                "/home",
                                "/home/**",
                                "/register",
                                "/verify-email/**",
                                "/forgot-password",
                                "/reset-password/**",
                                "/register-guest-seller",
                                "/verify-code-seller",
                                "/tools/**",
                                "/tools/{id}",
                                "/tools/{id}/**",
                                "/login",
                                "/register-guest-seller",
                                "/logout",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/uploads/**",
                                "/assets/**").permitAll()
                        // Phân quyền theo role
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/seller/**").hasRole("SELLER")
                        .requestMatchers("/moderator/**").hasRole("MOD")
                        .requestMatchers("/manager/**").hasRole("MANAGER")
                        // Các đường dẫn khác yêu cầu authenticated (đăng nhập)
                        .anyRequest().authenticated()
                )
                // Xử lý exception: 401 (unauthenticated) → redirect login
                // 403 (unauthorized role) → logout + redirect login
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(unauthenticatedHandler())
                        .accessDeniedHandler(accessDeniedHandler())
                )
                // Cấu hình logout
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                );

        return http.build();
    }

    // Xử lý 401 (chưa đăng nhập) - redirect /login
    @Bean
    public AuthenticationEntryPoint unauthenticatedHandler() {
        return (request, response, authException) -> {
            response.sendRedirect("/login?error=unauthenticated");
        };
    }

    // Xử lý 403 (đã đăng nhập nhưng sai role) - logout + redirect /login
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            // Logout session hiện tại
            request.getSession().invalidate();
            response.sendRedirect("/login?error=accessDenied&message=You do not have permission to access this page. Please log in with the appropriate role.");
        };
    }

//@Bean
//public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//    http
//            // Tắt xác thực CSRF để tránh lỗi POST form
//            .csrf(csrf -> csrf.disable())
//            .formLogin(form -> form.disable())
//            // Cho phép tất cả request không cần đăng nhập
//            .authorizeHttpRequests(auth -> auth
//                    .anyRequest().permitAll()
//            )
//
//            // Tắt hoàn toàn form login và logout
//            .formLogin(form -> form.disable())
//            .logout(logout -> logout.disable());
//
//    return http.build();
//}

    private void writeJsonError(HttpServletResponse response, String message, int status) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(status);
        Map<String, Object> error = new HashMap<>();
        error.put("error", message);
        error.put("status", status);
        new ObjectMapper().writeValue(response.getOutputStream(), error);
    }

    // Bật listener để Spring Security biết khi session hết hạn
    @Bean
    public ServletListenerRegistrationBean<HttpSessionEventPublisher> httpSessionEventPublisher() {
        return new ServletListenerRegistrationBean<>(new HttpSessionEventPublisher());
    }


}
