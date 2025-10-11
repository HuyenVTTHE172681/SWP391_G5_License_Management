package swp391.fa25.lms.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "License")
public class License {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long licenseId;

    @NotBlank(message = "License name cannot be blank")
    @Column(nullable = false)
    private String name;

    @ManyToOne
    @JoinColumn(name = "tool_id")
    private Tool tool;

    private Integer durationDays;

    private Double price;

    @OneToMany(mappedBy = "license")
    private List<CustomerOrder> customerOrders; // Nhiều order có thể chọn cùng 1 license

    private LocalDateTime createdAt;
}
