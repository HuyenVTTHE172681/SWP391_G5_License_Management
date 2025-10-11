package swp391.fa25.lms.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "Tool")
public class Tool {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tool_id")
    private Long toolId;

    @NotBlank(message = "Tool name cannot be blank")
    @Column(nullable = false)
    private String toolName;

    @NotBlank(message = "Image cannot be blank")
    @Column(nullable = false)
    private String image;

    @NotBlank(message = "Description cannot be blank")
    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @ManyToOne
    @JoinColumn(name = "seller_id")
    private Account seller;

    @NotNull(message = "Category cannot be null")
    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @Enumerated(EnumType.STRING)
    private Status status;
    public enum Status { PENDING, APPROVED, REJECTED }

    @OneToMany(mappedBy = "tool")
    private List<ToolFile> files;

    @OneToMany(mappedBy = "tool")
    private List<License> licenses;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
