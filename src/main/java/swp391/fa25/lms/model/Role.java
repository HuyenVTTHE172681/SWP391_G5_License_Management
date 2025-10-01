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
}
