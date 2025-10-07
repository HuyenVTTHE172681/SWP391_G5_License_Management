package swp391.fa25.lms.controller.auth;

import ch.qos.logback.core.model.Model;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.service.AccountService;


import java.util.HashMap;
import java.util.Map;
@RestController
public class AuthController {
    @Autowired
    private final AccountService accountService;

    public AuthController(AccountService accountService) {
        this.accountService = accountService;
    }


//    @PostMapping("/register")
//    public ResponseEntity<Map<String, Object>> register(@RequestBody Account account) {
//        try {
//            Account saved = accountService.registerAccount(account);
//
//            Map<String, Object> data = new HashMap<>();
//            data.put("accountId", saved.getAccountId());
//            data.put("fullName", saved.getFullName());
//            data.put("email", saved.getEmail());
//            data.put("verified", saved.getVerified());
//            data.put("role", saved.getRole() != null ? saved.getRole().getRoleName().name() : "UNKNOWN");
//            data.put("status", saved.getStatus());
//            data.put("createdAt", saved.getCreatedAt());
//            data.put("token", saved.getVerificationToken());
//
//            Map<String, Object> response = new HashMap<>();
//            response.put("status", 200);
//            response.put("message", "Registered successfully. Please check your email to verify.");
//            response.put("data", data);
//
//            return ResponseEntity.ok(response);
//        } catch (Exception e) {
//            Map<String, Object> error = new HashMap<>();
//            error.put("status", 400);
//            error.put("error", e.getMessage());
//            return ResponseEntity.badRequest().body(error);
//        }
//    }

//    @GetMapping("/verify-email/{token}")
//    public ResponseEntity<Map<String, Object>> verify(@PathVariable String token) {
//        Map<String, Object> response = new HashMap<>();
//        Map<String, Object> data = new HashMap<>();
//
//        try {
//            // Trường hợp token hợp lệ, tài khoản sẽ được xác minh
//            Account account = accountService.verifyAccount(token);
//
//            data.put("accountId", account.getAccountId());
//            data.put("fullName", account.getFullName());
//            data.put("email", account.getEmail());
//            data.put("verified", account.getVerified());
//            data.put("role", account.getRole() != null ? account.getRole().getRoleName().name() : null);
//            data.put("status", account.getStatus() != null ? account.getStatus().name() : null);
//
//            response.put("status", 200);
//            response.put("message", "Account verified successfully! You can now login.");
//            response.put("data", data);
//
//            return ResponseEntity.ok(response);
//
//        } catch (RuntimeException ex) {
//            // Trường hợp token không hợp lệ hoặc hết hạn
//            response.put("status", 400);
//            response.put("message", ex.getMessage());
//            response.put("data", new HashMap<>());
//
//            return ResponseEntity.badRequest().body(response);
//        }
//    }

//    @PostMapping("/login")
//    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> loginData) {
//        try {
//            String email = loginData.get("email");
//            String password = loginData.get("password");
//
//            Map<String, String> tokens = accountService.login(email, password);
//            Account account = accountService.getAccountByEmail(email);
//
//            Map<String, Object> data = new HashMap<>();
//            data.put("accountId", account.getAccountId());
//            data.put("fullName", account.getFullName());
//            data.put("email", account.getEmail());
//            data.put("accessToken", tokens.get("accessToken"));
//            data.put("refreshToken", tokens.get("refreshToken"));
//
//            Map<String, Object> response = new HashMap<>();
//            response.put("status", 200);
//            response.put("message", "Login successfully.");
//            response.put("data", data);
//
//            return ResponseEntity.ok(response);
//
//        } catch (RuntimeException e) {
//            Map<String, Object> error = new HashMap<>();
//            error.put("status", 400);
//            error.put("message", e.getMessage());
//            error.put("data", new HashMap<>());
//            return ResponseEntity.badRequest().body(error);
//        }
//    }
}
