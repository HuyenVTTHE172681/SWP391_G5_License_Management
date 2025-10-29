package swp391.fa25.lms.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"orders", "wallet"})
    private Account account; // buyer

    @ManyToOne
    @JoinColumn(name = "tool_id", nullable = false)
    @JsonIgnoreProperties({"orders", "licenses", "files", "seller", "category"})
    private Tool tool;

    @ManyToOne
    @JoinColumn(name = "license_id", nullable = false)
    @JsonIgnoreProperties({"customerOrders", "tool"})
    private License license;

    @Column(nullable = false)
    private Double price;

    public enum OrderStatus { PENDING, SUCCESS, FAILED }
    private OrderStatus orderStatus;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;
    public enum PaymentMethod { WALLET, BANK, PAYPAL }

    @ManyToOne
    @JoinColumn(name = "transaction_id")
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"wallet", "customerOrders", "licenseRenewLogs"})
    private WalletTransaction transaction;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL)
    private LicenseAccount licenseAccount;

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

    public OrderStatus getStatus() {
        return orderStatus;
    }

    public void setStatus(OrderStatus orderStatus) {
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


