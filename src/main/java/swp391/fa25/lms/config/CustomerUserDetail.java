package swp391.fa25.lms.config;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import swp391.fa25.lms.model.Account;
import java.util.Collection;
import java.util.List;

/**
 * Chuyển Account -> UserDetails
 * Lưu Account gốc để dùng khi cần
 */

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
        if (account.getRole() == null) return List.of();
        // Spring thường sử dụng prefix "ROLE_" cho SimpleGrantedAuthority
        return List.of(new SimpleGrantedAuthority("ROLE_" + account.getRole().getRoleName()));
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
        // Chỉ cho phép login nếu đã verify
        return Boolean.TRUE.equals(account.getVerified());
    }


}
