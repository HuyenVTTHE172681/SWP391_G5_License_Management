package swp391.fa25.lms.service.customer;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import swp391.fa25.lms.config.CustomerUserDetail;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.repository.AccountRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final AccountRepository accountRepo;

    public CustomUserDetailsService(AccountRepository accountRepo) {
        this.accountRepo = accountRepo;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Account account = accountRepo.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản với email: " + email));

        if (account.getStatus() == Account.AccountStatus.DEACTIVATED) {
            throw new UsernameNotFoundException("Tài khoản đã bị vô hiệu hóa");
        }

        if (Boolean.FALSE.equals(account.getVerified())) {
            throw new UsernameNotFoundException("Tài khoản chưa xác minh email");
        }

        return new CustomerUserDetail(account);
    }
}
