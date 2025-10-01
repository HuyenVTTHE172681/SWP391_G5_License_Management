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
    @JoinColumn(name = "account_id", unique = true)
    private Account account;

    private BigDecimal balance;
    private String currency = "VND";
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "wallet")
    private List<WalletTransaction> transactions;

    @OneToMany(mappedBy = "wallet")
    private List<WithdrawRequest> withdrawRequests;
}
