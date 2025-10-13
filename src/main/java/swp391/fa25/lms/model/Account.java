package swp391.fa25.lms.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "Account")
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_id")
    private Long accountId;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Định dạng email không hợp lệ")
    @Column(unique = true, nullable = false)
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    private String password;

    @NotBlank(message = "Họ và tên không được để trống")
    @Size(min = 10, max = 20, message = "Họ và tên đầy đủ phải từ 10 đến 20 ký tự")
    @Column(name = "fullName", columnDefinition = "NVARCHAR(100)")
    private String fullName;

    @Enumerated(EnumType.STRING)
    private AccountStatus status;
    public enum AccountStatus {
        ACTIVE, DEACTIVATED
    }

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Pattern(regexp = "0\\d{9}", message = "Số điện thoại phải có 9 chữ số bắt đầu bằng số 0")
    private String phone;

    @Column(columnDefinition = "NVARCHAR(100)")
    private String address;

    @Column(name = "is_verified")
    private Boolean verified = false;

    private String verificationToken;

    private LocalDateTime tokenExpiry;

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
    @JsonBackReference(value = "tool-seller")
    private List<Tool> tools;
}