package swp391.fa25.lms.model;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "Wallet")
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wallet_id")
    private Long walletId;

    @OneToOne
    @JoinColumn(name = "account_id", unique = true, nullable = false)
    private Account account;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal balance;
    private String currency = "VND";
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "wallet")
    private List<WalletTransaction> transactions;

    @OneToMany(mappedBy = "wallet")
    private List<WithdrawRequest> withdrawRequests;

    public Wallet() {
    }

    public Wallet(Long walletId, Account account, BigDecimal balance, String currency, LocalDateTime updatedAt, List<WalletTransaction> transactions, List<WithdrawRequest> withdrawRequests) {
        this.walletId = walletId;
        this.account = account;
        this.balance = balance;
        this.currency = currency;
        this.updatedAt = updatedAt;
        this.transactions = transactions;
        this.withdrawRequests = withdrawRequests;
    }

    public Long getWalletId() {
        return walletId;
    }

    public void setWalletId(Long walletId) {
        this.walletId = walletId;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<WalletTransaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<WalletTransaction> transactions) {
        this.transactions = transactions;
    }

    public List<WithdrawRequest> getWithdrawRequests() {
        return withdrawRequests;
    }

    public void setWithdrawRequests(List<WithdrawRequest> withdrawRequests) {
        this.withdrawRequests = withdrawRequests;
    }
}
