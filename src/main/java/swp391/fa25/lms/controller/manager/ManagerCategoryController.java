package swp391.fa25.lms.controller.manager;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import swp391.fa25.lms.model.Category;
import swp391.fa25.lms.service.moderator.CategoryService;

import java.util.List;

@Controller
@RequestMapping("/manager/category")
public class ManagerCategoryController {
    @Autowired
    private CategoryService categoryService;
    @GetMapping("/list")
    public String listCategories(@RequestParam(required = false) String name,
                                 @RequestParam(required = false) Category.Status status,
                                 Model model) {
        List<Category> categories = categoryService.filter(name, status);

        model.addAttribute("categories", categories);
        model.addAttribute("searchName", name);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("newCategory", new Category());
        return "manager/category-list";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("newCategory", new Category());
        return "manager/category-create";
    }

    @PostMapping("/create")
    public String createCategory(@Valid @ModelAttribute("newCategory") Category category,
                                 BindingResult result,
                                 Model model) {
        if (result.hasErrors()) {
            model.addAttribute("categories", categoryService.getAllCategories());
            return "manager/category-create";
        }
        if (categoryService.isExist(category.getCategoryName())){
            model.addAttribute("errMsg", "Category Name already exists");
            model.addAttribute("categorie", category);
            return "manager/category-create";
        }
        category.setStatus(Category.Status.ACTIVE);
        categoryService.save(category);
        return "redirect:/manager/category/list";
    }

    @GetMapping("/edit/{id}")
    public String editCategoryForm(@PathVariable("id") Long id, Model model) {
        Category category = categoryService.findById(id);
        model.addAttribute("category", category);
        return "manager/category-edit";
    }

    @PostMapping("/edit/{id}")
    public String updateCategory(@PathVariable("id") Long id,
                                 @Valid @ModelAttribute("category") Category category,
                                 BindingResult result) {
        if (result.hasErrors()) {
            return "manager/category-edit";
        }
        category.setCategoryId(id);
        categoryService.save(category);
        return "redirect:/manager/category/list";
    }

    @GetMapping("/delete/{id}")
    public String deactivateCategory(@PathVariable("id") Long id) {
        Category category = categoryService.findById(id);
        category.setStatus(Category.Status.DEACTIVATED);
        categoryService.save(category);
        return "redirect:/manager/category/list";
    }

    @GetMapping("/restore/{id}")
    public String restoreCategory(@PathVariable("id") Long id) {
        Category category = categoryService.findById(id);
        category.setStatus(Category.Status.ACTIVE);
        categoryService.save(category);
        return "redirect:/manager/category/list";
    }
}
