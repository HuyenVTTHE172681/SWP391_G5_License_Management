package swp391.fa25.lms.config;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import swp391.fa25.lms.model.Account;
import java.util.Collection;
import java.util.List;

public class CustomerUserDetail implements UserDetails {

    private final Account account;

    public CustomerUserDetail(Account account) {

        this.account = account;
        System.out.println("======= DEBUG CustomerUserDetail =======");
        System.out.println("Email: " + account.getEmail());
        System.out.println("Full name: " + account.getFullName());
        System.out.println("Role: " + (account.getRole() != null ? account.getRole().getRoleName() : "null"));
        System.out.println("Status: " + account.getStatus());
        System.out.println("Verified: " + account.getVerified());
        System.out.println("Password (encoded): " + account.getPassword());
        System.out.println("========================================");
    }

    public Account getAccount() {
        return account;
    }

//    @Override
//    public Collection<? extends GrantedAuthority> getAuthorities() {
//        // Lấy role name từ Account
//        return List.of(() -> account.getRole().getRoleName().name());
//    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (account.getRole() == null) return List.of();
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
//        return Boolean.TRUE.equals(account.getVerified());
        boolean enabled = Boolean.TRUE.equals(account.getVerified());
        System.out.println(">>> [DEBUG] isEnabled (verified=" + account.getVerified() + ") = " + enabled);
        return enabled;
    }


}
