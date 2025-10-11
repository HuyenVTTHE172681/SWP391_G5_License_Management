package swp391.fa25.lms.config;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import swp391.fa25.lms.model.Account;
import java.util.Collection;
import java.util.List;

public class CustomerUserDetail implements UserDetails {

    private final Account account;

    public CustomerUserDetail(Account account) {
        this.account = account;
    }

    public Account getAccount() {
        return account;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Lấy role name từ Account
        return List.of(() -> account.getRole().getRoleName().name());
    }

    @Override
    public String getPassword() {
        return account.getPassword();
    }

    @Override
    public String getUsername() {
        return account.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return account.getStatus() != Account.AccountStatus.DEACTIVATED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(account.getVerified());
    }
}
