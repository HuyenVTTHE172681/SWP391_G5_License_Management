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

    public Long getRenewLogId() {
        return renewLogId;
    }

    public void setRenewLogId(Long renewLogId) {
        this.renewLogId = renewLogId;
    }

    public LicenseAccount getLicenseAccount() {
        return licenseAccount;
    }

    public void setLicenseAccount(LicenseAccount licenseAccount) {
        this.licenseAccount = licenseAccount;
    }

    public LocalDateTime getRenewDate() {
        return renewDate;
    }

    public void setRenewDate(LocalDateTime renewDate) {
        this.renewDate = renewDate;
    }

    public LocalDateTime getNewEndDate() {
        return newEndDate;
    }

    public void setNewEndDate(LocalDateTime newEndDate) {
        this.newEndDate = newEndDate;
    }

    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(BigDecimal amountPaid) {
        this.amountPaid = amountPaid;
    }

    public WalletTransaction getTransaction() {
        return transaction;
    }

    public void setTransaction(WalletTransaction transaction) {
        this.transaction = transaction;
    }
}