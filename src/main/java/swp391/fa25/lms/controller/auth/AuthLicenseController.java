package swp391.fa25.lms.controller.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import swp391.fa25.lms.model.LicenseAccount;
import swp391.fa25.lms.repository.LicenseAccountRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/license")
public class AuthLicenseController {

    @Autowired
    private LicenseAccountRepository licenseAccountRepo;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {

        String username = body.get("username");
        String password = body.get("password");

        if (username == null || password == null)
            return ResponseEntity.badRequest().body("Missing username or password");

        // Tìm license account
        LicenseAccount acc = licenseAccountRepo.findByUsername(username)
                .orElse(null);

        if (acc == null)
            return ResponseEntity.status(401).body("Incorrect username");

        if (!acc.getPassword().equals(password))
            return ResponseEntity.status(401).body("Incorrect password");

        if (acc.getStatus() != LicenseAccount.Status.ACTIVE)
            return ResponseEntity.status(403).body("License inactive");

        // Check expired
        if (acc.getEndDate() != null && acc.getEndDate().isBefore(LocalDateTime.now()))
            return ResponseEntity.status(403).body("License expired");

        // Trả thông tin license/tool về cho WPF
        Map<String, Object> result = new HashMap<>();
        result.put("licenseId", acc.getLicenseAccountId());
        result.put("toolName", acc.getLicense().getTool().getToolName());
        result.put("startDate", acc.getStartDate());
        result.put("endDate", acc.getEndDate());
        result.put("licenseName", acc.getLicense().getName());
        result.put("method", acc.getLicense().getTool().getLoginMethod());

        return ResponseEntity.ok(result);
    }
}
