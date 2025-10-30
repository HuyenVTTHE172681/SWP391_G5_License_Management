package swp391.fa25.lms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp391.fa25.lms.model.Category;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category,Long> {
    Category findByCategoryId(Long categoryId);
    List<Category> findByCategoryName(String categoryName);
    List<Category> findByCategoryNameContainingIgnoreCase(String name);
    List<Category> findByStatus(Category.Status status);
    List<Category> findByCategoryNameContainingIgnoreCaseAndStatus(String name, Category.Status status);
    boolean existsByCategoryName(String categoryName);
}
