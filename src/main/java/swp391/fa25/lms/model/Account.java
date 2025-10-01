package swp391.fa25.lms.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "Account")
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_id")
    private Long accountId;

    @Column(unique = true, nullable = false)
    private String email;

    private String password;
    private String fullName;


    @Enumerated(EnumType.STRING)
    private AccountStatus status;
    public enum AccountStatus {
        ACTIVE, DEACTIVATED
    }

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String phone;
    private String address;

    @ManyToOne
    @JoinColumn(name = "role_id")
    private Role role;

    @OneToOne(mappedBy = "account", cascade = CascadeType.ALL)
    private Wallet wallet;

    // quan hệ với các bảng con
    @OneToMany(mappedBy = "account")
    private List<CustomerOrder> orders;

    @OneToMany(mappedBy = "account")
    private List<Feedback> feedbacks;

    @OneToMany(mappedBy = "account")
    private List<Favorite> favorites;

    @OneToMany(mappedBy = "seller")
    private List<Tool> tools;
}