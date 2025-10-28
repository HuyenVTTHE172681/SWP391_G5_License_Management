package swp391.fa25.lms.controller.common;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.Favorite;
import swp391.fa25.lms.model.Tool;
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

    // Count tool favorite
    @GetMapping("/count")
    @ResponseBody
    public ResponseEntity<Long> getFavoriteCount(HttpServletRequest request) {
        Account account = (Account) request.getSession().getAttribute("loggedInAccount");
        if (account == null) {
            return ResponseEntity.ok(0L);
        }

        long count = favoriteService.countFavoritesByAccount(account);
        return ResponseEntity.ok(count);
    }

    // List tool favorite
    @GetMapping("/list")
    public String getFavoriteList(HttpSession session, Model model) {
        Account account = (Account) session.getAttribute("loggedInAccount");
        if (account == null) {
            model.addAttribute("favorites", List.of());
            return "customer/favorite-list"; // vẫn render trang rỗng
        }
        List<Tool> favorites = favoriteService.getFavoritesByAccount(account.getAccountId());
        model.addAttribute("favorites", favorites);
        return "customer/favorite-list";
    }
}
