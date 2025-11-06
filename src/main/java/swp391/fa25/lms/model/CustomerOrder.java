package swp391.fa25.lms.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Customer_Order")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CustomerOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    @JsonIgnoreProperties({
            "orders", "wallet", "favorites", "feedbacks", "tools"
    })
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tool_id", nullable = false)
    @JsonIgnoreProperties({"orders", "licenses", "files", "seller", "category","feedbacks"})
    private Tool tool;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "license_id", nullable = false)
    @JsonIgnoreProperties({"customerOrders", "tool"})
    private License license;

    @Column(nullable = false)
    private Double price;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false)
    private OrderStatus orderStatus;
    public enum OrderStatus { PENDING, SUCCESS, FAILED }

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;
    public enum PaymentMethod { WALLET, BANK, PAYPAL }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    @JsonIgnoreProperties({"wallet", "customerOrders", "licenseRenewLogs"})
    private WalletTransaction transaction;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL)
    private LicenseAccount licenseAccount;

    // Add fields để lưu trạng thái hiển thị trong View
    @Transient
    private boolean canFeedbackOrReport;

    // THÊM MỚI: Lưu txnRef unique cho retry (nullable)
    @Column(name = "last_txn_ref")
    private String lastTxnRef;

    // THÊM MỚI: updatedAt cho update callback
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToOne(mappedBy = "order")
    @JsonIgnoreProperties({"order"})
    private Feedback feedback;

    public Feedback getFeedback() {
        return feedback;
    }

    public void setFeedback(Feedback feedback) {
        this.feedback = feedback;
    }

    public CustomerOrder() {
    }

    public CustomerOrder(Long orderId, Account account, Tool tool, License license, Double price, OrderStatus orderStatus, PaymentMethod paymentMethod, WalletTransaction transaction, LocalDateTime createdAt, LicenseAccount licenseAccount) {
        this.orderId = orderId;
        this.account = account;
        this.tool = tool;
        this.license = license;
        this.price = price;
        this.orderStatus = orderStatus;
        this.paymentMethod = paymentMethod;
        this.transaction = transaction;
        this.createdAt = createdAt;
        this.licenseAccount = licenseAccount;
    }

    // Getters/Setters (thêm cho lastTxnRef)
    public String getLastTxnRef() {
        return lastTxnRef;
    }

    public void setLastTxnRef(String lastTxnRef) {
        this.lastTxnRef = lastTxnRef;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public CustomerOrder(Account account, Tool tool, License license, Double price) {
        this.account = account;
        this.tool = tool;
        this.license = license;
        this.price = price;
        this.createdAt = LocalDateTime.now();
    }

    public boolean isCanFeedbackOrReport() {
        return canFeedbackOrReport;
    }

    public void setCanFeedbackOrReport(boolean canFeedbackOrReport) {
        this.canFeedbackOrReport = canFeedbackOrReport;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public Tool getTool() {
        return tool;
    }

    public void setTool(Tool tool) {
        this.tool = tool;
    }

    public License getLicense() {
        return license;
    }

    public void setLicense(License license) {
        this.license = license;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public OrderStatus getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(OrderStatus orderStatus) {
        this.orderStatus = orderStatus;
    }


    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public WalletTransaction getTransaction() {
        return transaction;
    }

    public void setTransaction(WalletTransaction transaction) {
        this.transaction = transaction;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LicenseAccount getLicenseAccount() {
        return licenseAccount;
    }

    public void setLicenseAccount(LicenseAccount licenseAccount) {
        this.licenseAccount = licenseAccount;
    }

//    Thêm method helper để check retryable
    public boolean isPending() {
        return OrderStatus.PENDING.equals(orderStatus);
    }

    // Thêm tính trung bình rating của seller
    @Transient
    private double sellerRating;

    public double getSellerRating() {
        return sellerRating;
    }

    public void setSellerRating(double sellerRating) {
        this.sellerRating = sellerRating;
    }

}


