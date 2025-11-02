package swp391.fa25.lms.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;


@Entity
@Table(name = "Tool")
@JsonIgnoreProperties({ "toolFiles"})
public class Tool {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tool_id")
    private Long toolId;

    @NotBlank(message = "Tool name cannot be blank")
    @Size(max = 100, message = "Tool name must be less than 100 characters")
    @Column(nullable = false, columnDefinition = "NVARCHAR(100)")
//    @Pattern(
//            regexp = "^[A-Za-z0-9]+(?: [A-Za-z0-9]+)*$",
//            message = "Tool name can only contain letters and numbers, separated by a single space"
//    )
    private String toolName;


    @Column(nullable = false)
    private String image;

    @NotBlank(message = "Description cannot be blank")
    @Size(max = 500, message = "Description must be under 500 characters")
    @Column(columnDefinition = "NVARCHAR(100)", nullable = false)
    private String description;

    @ManyToOne
    @JoinColumn(name = "seller_id")
    @com.fasterxml.jackson.annotation.JsonBackReference(value = "tool-seller")
    private Account seller;

    @Enumerated(EnumType.STRING)
    @Column(name = "login_method", nullable = false)
    private LoginMethod loginMethod;
    public enum LoginMethod {
        USER_PASSWORD,
        TOKEN
    }

    @NotNull(message = "Category cannot be null")
    @ManyToOne
    @JoinColumn(name = "category_id")
    @JsonBackReference(value = "tool-category")
    private Category category;

    @Enumerated(EnumType.STRING)
    private Status status;
    public enum Status { PENDING, APPROVED, REJECTED, PUBLISHED, DEACTIVATED }

    @OneToMany(mappedBy = "tool", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference(value = "tool-files")
    private List<ToolFile> files;

    @OneToMany(mappedBy = "tool", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties({"tool", "customerOrders"})
    private List<License> licenses;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "tool", cascade = CascadeType.ALL)
    @JsonIgnoreProperties({"tool", "license"})
    private List<CustomerOrder> orders;

    @Column(columnDefinition = "NVARCHAR(255)")
    private String note;

    @Column(nullable = false)
    private Integer quantity = 0;

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
    public Tool() {
    }

    public Tool(Long toolId, String toolName, String image, String description, Account seller, Category category, Status status, List<ToolFile> files, List<License> licenses, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.toolId = toolId;
        this.toolName = toolName;
        this.image = image;
        this.description = description;
        this.seller = seller;
        this.category = category;
        this.status = status;
        this.files = files;
        this.licenses = licenses;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getToolId() {
        return toolId;
    }

    public void setToolId(Long toolId) {
        this.toolId = toolId;
    }

    public @NotBlank(message = "Tool name cannot be blank") String getToolName() {
        return toolName;
    }

    public void setToolName(@NotBlank(message = "Tool name cannot be blank") String toolName) {
        this.toolName = toolName;
    }

    public List<CustomerOrder> getOrders() {
        return orders;
    }

    public void setOrders(List<CustomerOrder> orders) {
        this.orders = orders;
    }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public @NotBlank(message = "Description cannot be blank") String getDescription() {
        return description;
    }

    public void setDescription(@NotBlank(message = "Description cannot be blank") String description) {
        this.description = description;
    }

    public Account getSeller() {
        return seller;
    }

    public void setSeller(Account seller) {
        this.seller = seller;
    }

    public @NotNull(message = "Category cannot be null") Category getCategory() {
        return category;
    }

    public void setCategory(@NotNull(message = "Category cannot be null") Category category) {
        this.category = category;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public List<ToolFile> getFiles() {
        return files;
    }

    public void setFiles(List<ToolFile> files) {
        this.files = files;
    }

    public List<License> getLicenses() {
        return licenses;
    }

    public void setLicenses(List<License> licenses) {
        this.licenses = licenses;
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

    @Transient
    private Double averageRating;

    @Transient
    private Long totalReviews;

    public Double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }

    public Long getTotalReviews() {
        return totalReviews;
    }

    public void setTotalReviews(Long totalReviews) {
        this.totalReviews = totalReviews;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }


    public LoginMethod getLoginMethod() {
        return loginMethod;
    }

    public void setLoginMethod(LoginMethod loginMethod) {
        this.loginMethod = loginMethod;
    }

    public Tool(LoginMethod loginMethod) {
        this.loginMethod = loginMethod;
    }

    // ================== PRICE HELPERS ==================
    @Transient
    private BigDecimal minPrice;

    @Transient
    private BigDecimal maxPrice;

    public BigDecimal getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(BigDecimal minPrice) {
        this.minPrice = minPrice;
    }

    public BigDecimal getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(BigDecimal maxPrice) {
        this.maxPrice = maxPrice;
    }

    @Transient
    private boolean isFavorite;

    public boolean isIsFavorite() { return isFavorite; }
    public void setIsFavorite(boolean isFavorite) { this.isFavorite = isFavorite; }
}
