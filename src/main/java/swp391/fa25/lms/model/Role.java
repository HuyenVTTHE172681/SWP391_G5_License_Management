package swp391.fa25.lms.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "Role")
public class Role {
    @Id
    @Column(name = "role_id")
    private Integer roleId;

    @Enumerated(EnumType.STRING)
    private RoleName roleName;
    public enum RoleName {
        GUEST, CUSTOMER, SELLER, MOD, MANAGER, ADMIN
    }

    @Column(name = "note", columnDefinition = "NVARCHAR(100)")
    private String note;

    @OneToMany(mappedBy = "role")
    @JsonIgnore
    private List<Account> accounts;

    public Role() {
    }

    public Role(List<Account> accounts, String note, RoleName roleName, Integer roleId) {
        this.accounts = accounts;
        this.note = note;
        this.roleName = roleName;
        this.roleId = roleId;
    }

    public Integer getRoleId() {
        return roleId;
    }

    public void setRoleId(Integer roleId) {
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
