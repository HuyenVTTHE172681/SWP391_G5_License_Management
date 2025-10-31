package swp391.fa25.lms.model;

import jakarta.persistence.*;

@Entity
@Table(name = "seller_package")
public class SellerPackage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false)
    private String packageName;

    @Column(nullable = false)
    private int durationInMonths;

    @Column(nullable = false)
    private double price;

    @Column(nullable = true)
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
