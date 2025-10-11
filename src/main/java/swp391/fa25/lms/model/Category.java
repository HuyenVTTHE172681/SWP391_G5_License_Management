package swp391.fa25.lms.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

@Entity
@Table(name = "Category")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long categoryId;

    @NotBlank(message = "Category name cannot be blank")
    @Column(nullable = false)
    private String categoryName;

    private String description;

    @OneToMany(mappedBy = "category")
    private List<Tool> tools;
}
