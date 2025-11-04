package swp391.fa25.lms.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
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
    @Column(nullable = false, columnDefinition = "NVARCHAR(100)")
    private String categoryName;

    @Column(columnDefinition = "NVARCHAR(100)")
    private String description;

    @OneToMany(mappedBy = "category")
    @JsonBackReference(value = "tool-category")
    private List<Tool> tools;

    @Enumerated(EnumType.STRING)
    private Status status;

    public enum Status {ACTIVE, DEACTIVATED}

    private String icon;

    public Category() {
    }

    public Category(Long categoryId, String categoryName, String description, List<Tool> tools, String icon) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.description = description;
        this.tools = tools;
        this.icon = icon;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public @NotBlank(message = "Category name cannot be blank") String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(@NotBlank(message = "Category name cannot be blank") String categoryName) {
        this.categoryName = categoryName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Tool> getTools() {
        return tools;
    }

    public void setTools(List<Tool> tools) {
        this.tools = tools;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
