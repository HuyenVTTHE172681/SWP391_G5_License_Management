package swp391.fa25.lms.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import swp391.fa25.lms.config.CustomerUserDetail;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.repository.AccountRepo;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final AccountRepo accountRepo;

    public CustomUserDetailsService(AccountRepo accountRepo) {
        this.accountRepo = accountRepo;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Account account = accountRepo.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản với email: " + email));

        // Kiểm tra trạng thái tài khoản
        if (account.getStatus() == Account.AccountStatus.DEACTIVATED) {
            throw new UsernameNotFoundException("Tài khoản đã bị vô hiệu hóa");
        }
        if (Boolean.FALSE.equals(account.getVerified())) {
            throw new UsernameNotFoundException("Tài khoản chưa được xác minh email");
        }

        // ✅ Trả về CustomerUserDetail (bạn đã có class này)
        return new CustomerUserDetail(account);
    }
}
