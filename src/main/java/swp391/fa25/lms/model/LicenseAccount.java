package swp391.fa25.lms.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "License_Account")
public class LicenseAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long licenseAccountId;

    @NotBlank(message = "User name can not blank")
    @Column(nullable = false)
    private String username;

    @NotBlank(message = "Password name cannot be blank")
    @Column(nullable = false)
    private String password;

    @ManyToOne
    @JoinColumn(name = "license_id")
    private License license;

    @OneToOne
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private CustomerOrder order; // mỗi order sinh ra 1 license account

    @ManyToOne
    @JoinColumn(name = "tool_id")
    private Tool tool;

    @OneToMany(mappedBy = "licenseAccount")
    private List<LicenseRenewLog> renewAcc;

    @Enumerated(EnumType.STRING)
    private Status status;
    public enum Status { ACTIVE, EXPIRED, REVOKED }

    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime lastLogin;
    private String deviceInfo;
    private Integer maxDevices;

    // ===== NEW FIELDS =====
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoginMethod loginMethod = LoginMethod.USER_PASSWORD;
    public enum LoginMethod { USER_PASSWORD, TOKEN }

    @Column(unique = true)
    private String token;

    private Boolean used = false;
    private LocalDateTime activatedAt;

    public LicenseAccount() {
    }

    public LicenseAccount(Long licenseAccountId, String username, String password, License license, CustomerOrder order, Tool tool, List<LicenseRenewLog> renewAcc, Status status, LocalDateTime startDate, LocalDateTime endDate, LocalDateTime lastLogin, String deviceInfo, Integer maxDevices) {
        this.licenseAccountId = licenseAccountId;
        this.username = username;
        this.password = password;
        this.license = license;
        this.order = order;
        this.tool = tool;
        this.renewAcc = renewAcc;
        this.status = status;
        this.startDate = startDate;
        this.endDate = endDate;
        this.lastLogin = lastLogin;
        this.deviceInfo = deviceInfo;
        this.maxDevices = maxDevices;
    }

    public Long getLicenseAccountId() {
        return licenseAccountId;
    }

    public void setLicenseAccountId(Long licenseAccountId) {
        this.licenseAccountId = licenseAccountId;
    }

    public @NotBlank(message = "User name can not blank") String getUsername() {
        return username;
    }

    public void setUsername(@NotBlank(message = "User name can not blank") String username) {
        this.username = username;
    }

    public @NotBlank(message = "Password name cannot be blank") String getPassword() {
        return password;
    }

    public void setPassword(@NotBlank(message = "Password name cannot be blank") String password) {
        this.password = password;
    }

    public License getLicense() {
        return license;
    }

    public void setLicense(License license) {
        this.license = license;
    }

    public CustomerOrder getOrder() {
        return order;
    }

    public void setOrder(CustomerOrder order) {
        this.order = order;
    }

    public Tool getTool() {
        return tool;
    }

    public void setTool(Tool tool) {
        this.tool = tool;
    }

    public List<LicenseRenewLog> getRenewAcc() {
        return renewAcc;
    }

    public void setRenewAcc(List<LicenseRenewLog> renewAcc) {
        this.renewAcc = renewAcc;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
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

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    public String getDeviceInfo() {
        return deviceInfo;
    }

    public void setDeviceInfo(String deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

    public Integer getMaxDevices() {
        return maxDevices;
    }

    public void setMaxDevices(Integer maxDevices) {
        this.maxDevices = maxDevices;
    }

    public LoginMethod getLoginMethod() {
        return loginMethod;
    }

    public void setLoginMethod(LoginMethod loginMethod) {
        this.loginMethod = loginMethod;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Boolean getUsed() {
        return used;
    }

    public void setUsed(Boolean used) {
        this.used = used;
    }

    public LocalDateTime getActivatedAt() {
        return activatedAt;
    }

    public void setActivatedAt(LocalDateTime activatedAt) {
        this.activatedAt = activatedAt;
    }
}

