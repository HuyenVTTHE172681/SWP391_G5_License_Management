package swp391.fa25.lms.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "Account")
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_id")
    private Long accountId;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Định dạng email không hợp lệ")
    @Column(unique = true, nullable = false)
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    private String password;

    @NotBlank(message = "Họ và tên không được để trống")
    @Size(min = 5, max = 20, message = "Họ và tên đầy đủ phải từ 5 đến 20 ký tự")
    @Column(name = "fullName", columnDefinition = "NVARCHAR(100)")
    private String fullName;

    @Enumerated(EnumType.STRING)
    private AccountStatus status;
    public enum AccountStatus {
        ACTIVE, DEACTIVATED
    }

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Pattern(regexp = "0\\d{9}", message = "Số điện thoại phải có 9 chữ số bắt đầu bằng số 0")
    private String phone;

    @Column(columnDefinition = "NVARCHAR(100)")
    private String address;

    @Column(name = "is_verified")
    private Boolean verified = false;

    private String verificationToken;

    private LocalDateTime tokenExpiry;

    @Column(name = "verification_code")
    private String verificationCode;

    @Column(name = "code_expiry")
    private LocalDateTime codeExpiry;

    @Transient  // Không lưu vào database
    private String confirmPassword;

    @ManyToOne
    @JoinColumn(name = "role_id")
    private Role role;

    @OneToOne(mappedBy = "account", cascade = CascadeType.ALL)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"transactions"})
    private Wallet wallet;

    // quan hệ với các bảng con
    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"account", "license", "tool", "transaction"})
    private List<CustomerOrder> orders;

    @OneToMany(mappedBy = "account")
    private List<Feedback> feedbacks;

    @OneToMany(mappedBy = "account")
    private List<Favorite> favorites;

    @OneToMany(mappedBy = "seller")
    @com.fasterxml.jackson.annotation.JsonManagedReference(value = "tool-seller")
    private List<Tool> tools;

    @OneToMany(mappedBy = "uploadedBy")
    @JsonManagedReference(value = "file-uploader")
    private List<ToolFile> uploadedFiles;

    public Account() {
    }

    public Account(Long accountId, String email, String password, String fullName, AccountStatus status, LocalDateTime createdAt, LocalDateTime updatedAt, String phone, String address, Boolean verified, String verificationToken, LocalDateTime tokenExpiry, String verificationCode, LocalDateTime codeExpiry, Role role, Wallet wallet, List<CustomerOrder> orders, List<Feedback> feedbacks, List<Favorite> favorites, List<Tool> tools) {
        this.accountId = accountId;
        this.email = email;
        this.password = password;
        this.fullName = fullName;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.phone = phone;
        this.address = address;
        this.verified = verified;
        this.verificationToken = verificationToken;
        this.tokenExpiry = tokenExpiry;
        this.verificationCode = verificationCode;
        this.codeExpiry = codeExpiry;
        this.role = role;
        this.wallet = wallet;
        this.orders = orders;
        this.feedbacks = feedbacks;
        this.favorites = favorites;
        this.tools = tools;
    }

    public List<ToolFile> getUploadedFiles() {
        return uploadedFiles;
    }

    public void setUploadedFiles(List<ToolFile> uploadedFiles) {
        this.uploadedFiles = uploadedFiles;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public @NotBlank(message = "Email không được để trống") @Email(message = "Định dạng email không hợp lệ") String getEmail() {
        return email;
    }

    public void setEmail(@NotBlank(message = "Email không được để trống") @Email(message = "Định dạng email không hợp lệ") String email) {
        this.email = email;
    }

    public @NotBlank(message = "Mật khẩu không được để trống") String getPassword() {
        return password;
    }

    public void setPassword(@NotBlank(message = "Mật khẩu không được để trống") String password) {
        this.password = password;
    }

    public @NotBlank(message = "Họ và tên không được để trống") @Size(min = 5, max = 20, message = "Họ và tên đầy đủ phải từ 5 đến 20 ký tự") String getFullName() {
        return fullName;
    }

    public void setFullName(@NotBlank(message = "Họ và tên không được để trống") @Size(min = 5, max = 20, message = "Họ và tên đầy đủ phải từ 5 đến 20 ký tự") String fullName) {
        this.fullName = fullName;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public @Pattern(regexp = "0\\d{9}", message = "Số điện thoại phải có 9 chữ số bắt đầu bằng số 0") String getPhone() {
        return phone;
    }

    public void setPhone(@Pattern(regexp = "0\\d{9}", message = "Số điện thoại phải có 9 chữ số bắt đầu bằng số 0") String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Boolean getVerified() {
        return verified;
    }

    public void setVerified(Boolean verified) {
        this.verified = verified;
    }

    public String getVerificationToken() {
        return verificationToken;
    }

    public void setVerificationToken(String verificationToken) {
        this.verificationToken = verificationToken;
    }

    public LocalDateTime getTokenExpiry() {
        return tokenExpiry;
    }

    public void setTokenExpiry(LocalDateTime tokenExpiry) {
        this.tokenExpiry = tokenExpiry;
    }

    public String getVerificationCode() {
        return verificationCode;
    }

    public void setVerificationCode(String verificationCode) {
        this.verificationCode = verificationCode;
    }

    public LocalDateTime getCodeExpiry() {
        return codeExpiry;
    }

    public void setCodeExpiry(LocalDateTime codeExpiry) {
        this.codeExpiry = codeExpiry;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Wallet getWallet() {
        return wallet;
    }

    public void setWallet(Wallet wallet) {
        this.wallet = wallet;
    }

    public List<CustomerOrder> getOrders() {
        return orders;
    }

    public void setOrders(List<CustomerOrder> orders) {
        this.orders = orders;
    }

    public List<Feedback> getFeedbacks() {
        return feedbacks;
    }

    public void setFeedbacks(List<Feedback> feedbacks) {
        this.feedbacks = feedbacks;
    }

    public List<Favorite> getFavorites() {
        return favorites;
    }

    public void setFavorites(List<Favorite> favorites) {
        this.favorites = favorites;
    }

    public List<Tool> getTools() {
        return tools;
    }

    public void setTools(List<Tool> tools) {
        this.tools = tools;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}