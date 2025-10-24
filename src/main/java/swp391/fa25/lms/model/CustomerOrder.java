package swp391.fa25.lms.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Customer_Order")
public class CustomerOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long orderId;

    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account account; // buyer

    @ManyToOne
    @JoinColumn(name = "tool_id", nullable = false)
    private Tool tool;

    @ManyToOne
    @JoinColumn(name = "license_id", nullable = false)
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

    @ManyToOne
    @JoinColumn(name = "transaction_id")
    private WalletTransaction transaction;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL)
    private LicenseAccount licenseAccount;

    // Add fields để lưu trạng thái hiển thị trong View
    @Transient
    private boolean canFeedbackOrReport;

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
}


