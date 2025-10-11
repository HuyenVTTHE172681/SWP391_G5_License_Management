package swp391.fa25.lms.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class SecurityConfig {

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
                                "/", "/home", "/login", "/register",
                                "/verify-email/**", "/forgot-password", "/reset-password/**",
                                "/css/**", "/js/**", "/images/**", "/assets/**"
                        ).permitAll()
                        .requestMatchers("/profile/**").authenticated() // ✅ Chỉ user đăng nhập mới truy cập
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .defaultSuccessUrl("/home", true)
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
