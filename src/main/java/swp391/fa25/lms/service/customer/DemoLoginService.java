package swp391.fa25.lms.service.customer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import swp391.fa25.lms.model.LicenseAccount;
import swp391.fa25.lms.repository.LicenseAccountRepository;

import java.util.List;
import java.util.Optional;

@Service
public class DemoLoginService {
    @Autowired
    LicenseAccountRepository licenseAccountRepository;

    public List<LicenseAccount> getAllLicenseAccounts(long toolId) {
        return licenseAccountRepository.findByLicense_Tool_ToolId(toolId);
    }
    public Optional<LicenseAccount> findByTokenAndTool(String token, long toolId) {
        return licenseAccountRepository.findByTokenAndLicense_Tool_ToolId(token, toolId);
    }
    public Optional<LicenseAccount> findByUsernamePasswordAndTool(String username, String password, long toolId) {
        return licenseAccountRepository.findByUsernameAndPasswordAndLicense_Tool_ToolId(username, password, toolId);
    }
}
