package swp391.fa25.lms.model;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "Role")
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Long roleId;

    @Enumerated(EnumType.STRING)
    private RoleName roleName;
    public enum RoleName {
        GUEST, CUSTOMER, SELLER, MOD, MANAGER, ADMIN
    }

    private String note;

    @OneToMany(mappedBy = "role")
    private List<Account> accounts;

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public RoleName getRoleName() {
        return roleName;
    }

    public void setRoleName(RoleName roleName) {
        this.roleName = roleName;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public List<Account> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<Account> accounts) {
        this.accounts = accounts;
    }
}
