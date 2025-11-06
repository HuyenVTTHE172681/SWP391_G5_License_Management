package swp391.fa25.lms.service.moderator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import swp391.fa25.lms.model.LicenseAccount;
import swp391.fa25.lms.repository.LicenseAccountRepository;

import java.util.List;

@Service("moderatorLicenseAccountService")
public class LicenseAccountService {
    @Autowired
    private LicenseAccountRepository licenseAccountRepository;

    public List<LicenseAccount> findByToolId(long toolId) {
        return licenseAccountRepository.findByLicense_Tool_ToolId(toolId);
    }
}
