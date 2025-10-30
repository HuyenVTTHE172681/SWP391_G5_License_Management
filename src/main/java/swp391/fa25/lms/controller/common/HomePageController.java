package swp391.fa25.lms.controller.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.Tool;
import swp391.fa25.lms.service.customer.CategoryService;
import swp391.fa25.lms.service.customer.ToolService;

@Controller
public class HomePageController {

    @Autowired
    private CategoryService categoryService;
    @Autowired
    private ToolService toolService;

    @GetMapping("/")
    public String defaultRedirect() {
        // Default => redirect tới /home
        return "redirect:/home";
    }

    // Home
    @GetMapping("/home")
    public String showHomePage(
            HttpServletRequest request,
            Model model) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Account account = (Account) request.getSession().getAttribute("loggedInAccount");

        // Add vao model
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("account", account);
        model.addAttribute("maskedPassword", request.getSession().getAttribute("maskedPassword"));
        return "public/home";
    }

    // API load fragment danh sách san pham (AJAX)
    @GetMapping("/home/tools")
    public String getFilteredTools(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "dateFilter", required = false) String dateFilter,
            @RequestParam(value = "page", defaultValue = "0") int page,
            Model model) {

        int size = 9;
        Page<Tool> toolPage = toolService.searchAndFilterTools(keyword, categoryId, dateFilter, page, size);

        model.addAttribute("tools", toolPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", toolPage.getTotalPages());
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedCategory", categoryId);
        model.addAttribute("dateFilter", dateFilter);

        // Trả về fragment Thymeleaf
        return "public/tool-list :: toolList";
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request) {
        request.getSession().invalidate();
        return "redirect:/login";
    }

//    @GetMapping("/logout-success")
//    public String logoutSuccess(Model model) {
//        model.addAttribute("message", "Bạn đã đăng xuất thành công");
//        return "public/login";
//    }
}
