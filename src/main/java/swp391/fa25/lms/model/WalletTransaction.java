package swp391.fa25.lms.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "Wallet_Transaction")
public class WalletTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long transactionId;

    @ManyToOne
    @JoinColumn(name = "wallet_id")
    @com.fasterxml.jackson.annotation.JsonBackReference(value = "wallet-trans")
    private Wallet wallet;

    @Enumerated(EnumType.STRING)
    private TransactionType transactionType;
    public enum TransactionType { DEPOSIT, BUY, RENEW, WITHDRAW }

    @Enumerated(EnumType.STRING)
    private TransactionStatus status;
    public enum TransactionStatus { PENDING, SUCCESS, FAILED }

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @OneToMany(mappedBy = "transaction")
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"transaction", "wallet"})
    private List<CustomerOrder> customerOrders;

    @OneToMany(mappedBy = "transaction")
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"transaction"})
    private List<LicenseRenewLog> licenseRenewLogs;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    // THÊM MỚI: updatedAt cho update callback
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public WalletTransaction() {
    }

    public WalletTransaction(Long transactionId, Wallet wallet, TransactionType transactionType, TransactionStatus status, BigDecimal amount, List<CustomerOrder> customerOrders, List<LicenseRenewLog> licenseRenewLogs, LocalDateTime createdAt) {
        this.transactionId = transactionId;
        this.wallet = wallet;
        this.transactionType = transactionType;
        this.status = status;
        this.amount = amount;
        this.customerOrders = customerOrders;
        this.licenseRenewLogs = licenseRenewLogs;
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public Wallet getWallet() {
        return wallet;
    }

    public void setWallet(Wallet wallet) {
        this.wallet = wallet;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public List<CustomerOrder> getCustomerOrders() {
        return customerOrders;
    }

    public void setCustomerOrders(List<CustomerOrder> customerOrders) {
        this.customerOrders = customerOrders;
    }

    public List<LicenseRenewLog> getLicenseRenewLogs() {
        return licenseRenewLogs;
    }

    public void setLicenseRenewLogs(List<LicenseRenewLog> licenseRenewLogs) {
        this.licenseRenewLogs = licenseRenewLogs;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
