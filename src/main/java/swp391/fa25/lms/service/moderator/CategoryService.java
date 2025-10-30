package swp391.fa25.lms.service.moderator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import swp391.fa25.lms.model.Category;
import swp391.fa25.lms.repository.CategoryRepository;

import java.util.List;
@Service("moderatorCategoryService")
public class CategoryService {
    @Autowired
    private CategoryRepository categoryRepo;

    public List<Category> getAllCategories() {
        return categoryRepo.findAll();
    }
    public List<Category> searchByName(String name) {
        return categoryRepo.findByCategoryName(name);
    }
    public void save(Category category) {
        categoryRepo.save(category);
    }
    public List<Category> filter(String name, Category.Status status) {
        if ((name == null || name.trim().isBlank()) && status == null)
            return categoryRepo.findAll();

        if (status == null)
            return categoryRepo.findByCategoryNameContainingIgnoreCase(name);

        if (name == null || name.isBlank())
            return categoryRepo.findByStatus(status);

        return categoryRepo.findByCategoryNameContainingIgnoreCaseAndStatus(name, status);
    }

    public Category findById(Long id) {
        return categoryRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid category ID: " + id));
    }
    public boolean isExist(String name) {
        return categoryRepo.existsByCategoryName(name);
    }
}
