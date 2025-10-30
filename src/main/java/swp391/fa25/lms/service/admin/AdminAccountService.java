package swp391.fa25.lms.service.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.Role;
import swp391.fa25.lms.model.Role.RoleName;
import swp391.fa25.lms.repository.AccountRepository;
import swp391.fa25.lms.repository.RoleRepository;

@Service("adminAccountService")
public class AdminAccountService {

    private static final String FIXED_ADMIN_EMAIL = "admin@gmail.com";

    @Autowired
    private AccountRepository accountRepo;

    @Autowired
    private RoleRepository roleRepo;

    /** List/Search cho accounts.html */
    public Page<Account> search(String q, Pageable pageable) {
        if (q == null || q.isBlank()) {
            return accountRepo.findAll(pageable);
        }
        return accountRepo.findByEmailContainingIgnoreCaseOrFullNameContainingIgnoreCase(q, q, pageable);
    }

    /** Lấy chi tiết account (ném lỗi nếu không thấy) */
    public Account get(long id) {
        return accountRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + id));
    }

    /** Đổi role với rule ADMIN cố định */
    public void changeRole(long accountId, RoleName newRole) {
        Account acc = get(accountId);

        if (FIXED_ADMIN_EMAIL.equalsIgnoreCase(acc.getEmail()) && newRole != RoleName.ADMIN) {
            throw new IllegalStateException("Cannot change role of the fixed admin account.");
        }
        if (!FIXED_ADMIN_EMAIL.equalsIgnoreCase(acc.getEmail()) && newRole == RoleName.ADMIN) {
            throw new IllegalStateException("Only account " + FIXED_ADMIN_EMAIL + " can have ADMIN role.");
        }

        Role role = roleRepo.findByRoleName(newRole)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + newRole));

        acc.setRole(role);
        accountRepo.save(acc);
    }

    /** Xoá account (không cho xoá admin cố định) */
    public void delete(long accountId) {
        Account acc = get(accountId);
        if (FIXED_ADMIN_EMAIL.equalsIgnoreCase(acc.getEmail())) {
            throw new IllegalStateException("Cannot delete the fixed admin account.");
        }
        accountRepo.deleteById(accountId);
    }
}
