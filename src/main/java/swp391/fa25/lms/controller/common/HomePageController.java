package swp391.fa25.lms.controller.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.LicenseAccount;
import swp391.fa25.lms.model.Tool;
import swp391.fa25.lms.repository.LicenseAccountRepository;
import swp391.fa25.lms.service.customer.CategoryService;
import swp391.fa25.lms.service.customer.FavoriteService;
import swp391.fa25.lms.service.customer.LicenseAccountService;
import swp391.fa25.lms.service.customer.ToolService;

import java.util.List;

@Controller
public class HomePageController {

    @Autowired
    private CategoryService categoryService;
    @Autowired
    private ToolService toolService;
    @Autowired
    private FavoriteService favoriteService;

    @Autowired
    private LicenseAccountRepository licenseAccountRepository;
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
            @RequestParam(value = "priceFilter", required = false) String priceFilter,
            @RequestParam(value = "ratingFilter", required = false) Integer ratingFilter,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "9") int size,
            HttpServletRequest request,
            Model model) {

        // Lấy account từ session
        Account account = (Account) request.getSession().getAttribute("loggedInAccount");

        Page<Tool> toolPage = toolService.searchAndFilterTools(
                keyword, categoryId, dateFilter, priceFilter, ratingFilter, account, page, size
        );
        for (Tool tool : toolPage.getContent()) {
            List<LicenseAccount> active = licenseAccountRepository.findByStatusAndLicense_Tool_ToolId(LicenseAccount.Status.ACTIVE, tool.getToolId());
            int origin = tool.getQuantity() == null ? 0 : tool.getQuantity();
            int remain = origin - active.size();
            tool.setAvailableQuantity(Math.max(remain, 0));
        }

        model.addAttribute("tools", toolPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", toolPage.getTotalPages());
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedCategory", categoryId);
        model.addAttribute("dateFilter", dateFilter);
        model.addAttribute("priceFilter", priceFilter);
        model.addAttribute("ratingFilter", ratingFilter);
        model.addAttribute("pageSize", size);

        // Render fragment trong home.html
        return "public/home :: toolList";
    }


    @GetMapping("/logout")
    public String logout(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            new SecurityContextLogoutHandler().logout(request, null, auth);  // Clear SecurityContext
        }
        request.getSession().invalidate();  // Invalidate session (giữ nguyên)
        return "redirect:/login";
    }
}
