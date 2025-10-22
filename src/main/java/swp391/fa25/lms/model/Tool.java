package swp391.fa25.lms.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "Tool")
@JsonIgnoreProperties({"seller", "toolFiles"})
public class Tool {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tool_id")
    private Long toolId;

    @NotBlank(message = "Tool name cannot be blank")
    @Column(nullable = false, columnDefinition = "NVARCHAR(100)")
    private String toolName;

    @NotBlank(message = "Image cannot be blank")
    @Column(nullable = false)
    private String image;

    @NotBlank(message = "Description cannot be blank")
    @Column(columnDefinition = "NVARCHAR(100)", nullable = false)
    private String description;

    @ManyToOne
    @JoinColumn(name = "seller_id")
    @JsonManagedReference(value = "tool-seller")
    private Account seller;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tool_login_methods", joinColumns = @JoinColumn(name = "tool_id"))
    @Column(name = "login_method")
    private Set<String> loginMethods = new HashSet<>();

    @NotNull(message = "Category cannot be null")
    @ManyToOne
    @JoinColumn(name = "category_id")
    @JsonManagedReference(value = "tool-category")
    private Category category;


    @Enumerated(EnumType.STRING)
    private Status status;

    public enum Status {PENDING, APPROVED, REJECTED, PUBLISH, DEACTIVE}

    @OneToMany(mappedBy = "tool", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<ToolFile> files;

    @OneToMany(mappedBy = "tool", cascade = CascadeType.ALL)
    @JsonManagedReference(value = "tool-licenses")
    private List<License> licenses;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @Column(nullable = false)
    private Integer quantity = 0;

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

    public Set<String> getLoginMethods() {
        return loginMethods;
    }

    public void setLoginMethods(Set<String> loginMethods) {
        this.loginMethods = loginMethods;
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

    public @NotBlank(message = "Image cannot be blank") String getImage() {
        return image;
    }

    public void setImage(@NotBlank(message = "Image cannot be blank") String image) {
        this.image = image;
    }

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
}
