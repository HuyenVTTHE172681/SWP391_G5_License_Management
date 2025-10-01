package swp391.fa25.lms.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "License_Renew_Log")
public class LicenseRenewLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long renewLogId;

    @ManyToOne
    @JoinColumn(name = "license_account_id")
    private LicenseAccount licenseAccount;

    private LocalDateTime renewDate;
    private LocalDateTime newEndDate;
    private BigDecimal amountPaid;

    @ManyToOne
    @JoinColumn(name = "transaction_id")
    private WalletTransaction transaction;
}