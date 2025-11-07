package swp391.fa25.lms.controller.common;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.Favorite;
import swp391.fa25.lms.model.Tool;
import swp391.fa25.lms.service.customer.CategoryService;
import swp391.fa25.lms.service.customer.FavoriteService;
import swp391.fa25.lms.service.customer.ToolService;

import jakarta.servlet.http.HttpServletRequest;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/favorite")
public class FavoriteController {

    @Autowired
    private FavoriteService favoriteService;

    @Autowired
    private ToolService toolService;
    @Autowired
    private CategoryService categoryService;

    /**
     * Toggle yêu thích tool
     */
    @PostMapping("/toggle")
    @ResponseBody
    public ResponseEntity<?> toggleFavorite(
            @RequestParam("toolId") Long toolId,
            HttpServletRequest request
    ) {
        // Lấy account từ session
        Account account = (Account) request.getSession().getAttribute("loggedInAccount");
        if (account == null) {
            return ResponseEntity.status(401).body("Bạn cần đăng nhập để yêu thích tool!");
        }

        Tool tool = toolService.getToolById(toolId);  // sử dụng method getToolById() có trong ToolService
        if (tool == null) {
            return ResponseEntity.badRequest().body("Tool không tồn tại!");
        }

        boolean added = favoriteService.toggleFavorite(account, tool);

        return ResponseEntity.ok(added ? "added" : "removed");
    }

    // Count tool favorite dùng ID
    @GetMapping("/count")
    @ResponseBody
    public ResponseEntity<Long> getFavoriteCount(HttpServletRequest request) {
        Account account = (Account) request.getSession().getAttribute("loggedInAccount");
        if (account == null || account.getAccountId() == null) {
            return ResponseEntity.ok(0L);
        }

        long count = favoriteService.countByAccountId(account.getAccountId());
        return ResponseEntity.ok(count);
    }

    // List tool favorite
    @GetMapping("/list")
    public String getFavoriteList(HttpSession session, Model model) {
        Account account = (Account) session.getAttribute("loggedInAccount");
        if (account == null) {
            model.addAttribute("favorites", List.of());
            model.addAttribute("favoriteCount", 0L);
            model.addAttribute("categories", List.of());// Thêm count
            return "customer/favorite-list";
        }
        List<Tool> favorites = favoriteService.getFavoritesByAccount(account.getAccountId());
        long count = favorites.size();
        model.addAttribute("favorites", favorites);
        model.addAttribute("favoriteCount", count);  // Thêm vào model
        model.addAttribute("categories", categoryService.getAllCategories());

        return "customer/favorite-list";
    }

    @GetMapping("/tools")
    public String getFilteredFavorites(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "dateFilter", required = false) String dateFilter,
            @RequestParam(value = "priceFilter", required = false) String priceFilter,
            @RequestParam(value = "ratingFilter", required = false) Integer ratingFilter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            HttpSession session,
            Model model) {

        Account account = (Account) session.getAttribute("loggedInAccount");
        if (account == null) {
            model.addAttribute("favorites", List.of());
            model.addAttribute("totalPages", 0);
            return "customer/favorite-list :: favoriteToolList";
        }

        // Lấy all favorites trước
        List<Tool> allFavorites = favoriteService.getFavoritesByAccount(account.getAccountId());

        // Filter + paginate
        Page<Tool> filteredPage = favoriteService.searchAndFilterFavorites(
                allFavorites, keyword, categoryId, dateFilter, priceFilter, ratingFilter, page, size);

        model.addAttribute("favorites", filteredPage.getContent());
        model.addAttribute("totalPages", filteredPage.getTotalPages());
        model.addAttribute("currentPage", filteredPage.getNumber());
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedCategory", categoryId);
        model.addAttribute("dateFilter", dateFilter);
        model.addAttribute("priceFilter", priceFilter);
        model.addAttribute("ratingFilter", ratingFilter);
        model.addAttribute("pageSize", size);
        model.addAttribute("favoriteCount", allFavorites.size());

        // Render fragment
        return "customer/favorite-list :: favoriteToolList";
    }
}
