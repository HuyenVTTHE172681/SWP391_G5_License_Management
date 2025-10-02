package swp391.fa25.lms.controller.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.service.AccountService;

import java.util.HashMap;
import java.util.Map;
@RestController
public class AuthController {
    private final AccountService accountService;

    public AuthController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody Account account) {
        Account saved = accountService.registerAccount(account);

        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.OK.value());
        response.put("message", "Registered successfully. Please check your email to verify.");

        // Đưa ra một số field cần thiết (không trả password)
        Map<String, Object> accountData = new HashMap<>();
        accountData.put("id", saved.getAccountId());  // giả sử field là accountId
        accountData.put("email", saved.getEmail());
        accountData.put("fullName", saved.getFullName());
        accountData.put("verified", saved.getVerified());

        response.put("data", accountData);

        return new ResponseEntity<>(response.toString(), HttpStatus.OK);
    }

    @GetMapping("/verify-email/{token}")
    public ResponseEntity<String> verify(@PathVariable String token) {
        String result = accountService.verifyAccount(token);
        return ResponseEntity.ok(result);
    }

}
