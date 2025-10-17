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

    // 🔹 Người mua (customer)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    // 🔹 Tool mà customer mua (thuộc về một seller)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tool_id", nullable = false)
    private Tool tool;

    // 🔹 License mà buyer chọn
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "license_id", nullable = false)
    private License license;

    // 🔹 Giá tại thời điểm mua
    @Column(nullable = false)
    private Double price;

    // 🔹 Phương thức thanh toán
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod = PaymentMethod.QR_CODE;
    public enum PaymentMethod {
        QR_CODE, BANK_TRANSFER, PAYPAL
    }

    // 🔹 Trạng thái thanh toán
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;
    public enum PaymentStatus {
        PENDING, PAID, FAILED, CANCELLED
    }

    // 🔹 Đường dẫn / ảnh QR code (nếu dùng thanh toán QR)
    @Column(name = "qr_code_url")
    private String qrCodeUrl;

    // 🔹 Mã giao dịch hoặc reference từ bên thanh toán (nếu có)
    @Column(name = "payment_ref")
    private String paymentRef;

    // 🔹 Thời điểm tạo và thanh toán
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    // 🔹 LicenseAccount được cấp sau khi thanh toán thành công
    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private LicenseAccount licenseAccount;

    // ======= Constructors =======
    public CustomerOrder() {}

    public CustomerOrder(Account account, Tool tool, License license, Double price) {
        this.account = account;
        this.tool = tool;
        this.license = license;
        this.price = price;
        this.createdAt = LocalDateTime.now();
    }

    // ======= Helper =======
    public boolean isPaid() {
        return this.paymentStatus == PaymentStatus.PAID;
    }

    // ======= Getters & Setters =======
    public Long getOrderId() { return orderId; }

    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }

    public Tool getTool() { return tool; }
    public void setTool(Tool tool) { this.tool = tool; }

    public License getLicense() { return license; }
    public void setLicense(License license) { this.license = license; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getQrCodeUrl() { return qrCodeUrl; }
    public void setQrCodeUrl(String qrCodeUrl) { this.qrCodeUrl = qrCodeUrl; }

    public String getPaymentRef() { return paymentRef; }
    public void setPaymentRef(String paymentRef) { this.paymentRef = paymentRef; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }

    public LicenseAccount getLicenseAccount() { return licenseAccount; }
    public void setLicenseAccount(LicenseAccount licenseAccount) {
        this.licenseAccount = licenseAccount;
        if (licenseAccount != null) licenseAccount.setOrder(this);
    }
}
