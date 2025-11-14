package swp391.fa25.lms.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "seller_package")
public class SellerPackage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    private int id;
    @Column(nullable = false, columnDefinition = "NVARCHAR(100)")
    @NotBlank(message = "Package name cannot be blank")
    @Size(min = 3, max = 100, message = "Package name must be between 3 and 100 characters")
    @Pattern(
            regexp = "^(?!.* {2,}).+$",
            message = "Package name must not contain consecutive spaces"
    )
    private String packageName;

    @Column(nullable = false)
    @Min(value = 1, message = "Duration must be at least 1 month")
    @Max(value = 36, message = "Duration must not exceed 36 months")
    private int durationInMonths;

    @Column(nullable = false)
    @DecimalMin(value = "0.01", inclusive = true, message = "Price must be at least 0.01")
    @DecimalMax(value = "100000000", inclusive = true, message = "Price must not exceed 100000000")
    private double price;

    @Column(nullable = true, columnDefinition = "NVARCHAR(100)")
    @Size(max = 100, message = "Description must be at most 100 characters")
    @Pattern(
            regexp = "^(?!.* {2,}).*$",
            message = "Description must not contain consecutive spaces"
    )
    private String description;

    @Enumerated(EnumType.STRING)
    private Status status;

    public enum Status {ACTIVE, DEACTIVATED}

    public SellerPackage() {
    }

    public SellerPackage(int id, String packageName, int durationInMonths, double price, String description) {
        this.id = id;
        this.packageName = packageName;
        this.durationInMonths = durationInMonths;
        this.price = price;
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public int getDurationInMonths() {
        return durationInMonths;
    }

    public void setDurationInMonths(int durationInMonths) {
        this.durationInMonths = durationInMonths;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
