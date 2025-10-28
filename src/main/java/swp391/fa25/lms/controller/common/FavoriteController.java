package swp391.fa25.lms.controller.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.Favorite;
import swp391.fa25.lms.model.Tool;
import swp391.fa25.lms.service.customer.FavoriteService;
import swp391.fa25.lms.service.customer.ToolService;

import jakarta.servlet.http.HttpServletRequest;

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
}
