package swp391.fa25.lms.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "License_Account")
public class LicenseAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long licenseAccountId;

    @NotBlank(message = "User name can not blank")
    @Column(nullable = false)
    private String username;

    @NotBlank(message = "Password name cannot be blank")
    @Column(nullable = false)
    private String password;

    @ManyToOne
    @JoinColumn(name = "license_id")
    private License license;

    @OneToOne
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private CustomerOrder order; // mỗi order sinh ra 1 license account

    @ManyToOne
    @JoinColumn(name = "tool_id")
    private Tool tool;

    @OneToMany(mappedBy = "licenseAccount")
    private List<LicenseRenewLog> renewAcc;

    @Enumerated(EnumType.STRING)
    private Status status;
    public enum Status { ACTIVE, EXPIRED, REVOKED }

    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime lastLogin;
    private String deviceInfo;
    private Integer maxDevices;
}
