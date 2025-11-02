package swp391.fa25.lms.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "seller_subscription")
public class SellerSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    // ✅ Liên kết tới tài khoản
    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    // ✅ Liên kết tới gói seller
    @ManyToOne
    @JoinColumn(name = "package_id", nullable = false)
    private SellerPackage sellerPackage;

    // ✅ Thời gian gia hạn
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    // ✅ Giá trị tiền (ghi lại tại thời điểm mua)
    private double priceAtPurchase;

    // ✅ Trạng thái (đang hoạt động / đã hết hạn)
    private boolean active;

    public SellerSubscription() {}

    public SellerSubscription(int id, Account account, SellerPackage sellerPackage, LocalDateTime startDate, LocalDateTime endDate, double priceAtPurchase, boolean active) {
        this.id = id;
        this.account = account;
        this.sellerPackage = sellerPackage;
        this.startDate = startDate;
        this.endDate = endDate;
        this.priceAtPurchase = priceAtPurchase;
        this.active = active;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public SellerPackage getSellerPackage() {
        return sellerPackage;
    }

    public void setSellerPackage(SellerPackage sellerPackage) {
        this.sellerPackage = sellerPackage;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public double getPriceAtPurchase() {
        return priceAtPurchase;
    }

    public void setPriceAtPurchase(double priceAtPurchase) {
        this.priceAtPurchase = priceAtPurchase;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }


}
