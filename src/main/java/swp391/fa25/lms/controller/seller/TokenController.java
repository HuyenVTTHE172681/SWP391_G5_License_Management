package swp391.fa25.lms.controller.seller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import swp391.fa25.lms.service.seller.TokenService;
import swp391.fa25.lms.service.seller.ToolFlowService;

import java.io.IOException;
import java.util.List;

@Controller
@RequestMapping("/seller/token-manage")
@Validated
public class TokenController {

    @Autowired
    private ToolFlowService toolFlowService;
    @Autowired private TokenService tokenService;
    /**
     * ✅ Hiển thị trang token-manage
     */
    @GetMapping
    public String showTokenManage() {
        return "seller/token-manage";
    }

    /**
     * ✅ Xử lý khi người dùng nhập token và bấm Submit
     */
    @PostMapping("/submit")
    public String submitTokens(
            @RequestParam("tokens")
            List<@Pattern(regexp = "^[0-9]{6}$", message = "Each token must contain exactly 6 digits") String> tokens,
            HttpSession session,
            RedirectAttributes redirectAttrs) {

        try {
            toolFlowService.finalizeTokenTool(tokens, session);
            redirectAttrs.addFlashAttribute("success", "Tool created successfully!");
            return "redirect:/seller/tools";
        } catch (IOException e) {
            redirectAttrs.addFlashAttribute("error", "File error: " + e.getMessage());
            return "redirect:/seller/token-manage";
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
            return "redirect:/seller/token-manage";
        }
    }

    /**
     * ✅ Khi người dùng bấm nút “Back” → xóa session và trở lại form add tool
     */
    @PostMapping("/back")
    public String handleBack(HttpSession session, RedirectAttributes redirectAttrs) {
        toolFlowService.cancelToolCreation(session);
        redirectAttrs.addFlashAttribute("info", "Tool creation canceled. Returning to add form.");
        return "redirect:/seller/tools/add";
    }

    /**
     * ✅ Thêm token cho tool đã có trong DB (DB mode)
     */
    @PostMapping("/{toolId}/add")
    public String addTokensToExistingTool(
            @PathVariable Long toolId,
            @RequestParam("tokens")
            List<@Pattern(regexp = "^[0-9]{6}$", message = "Each token must contain exactly 6 digits") String> tokens,
            HttpSession session,
            RedirectAttributes redirectAttrs) {

        try {
            var seller = (swp391.fa25.lms.model.Account) session.getAttribute("loggedInUser");
            tokenService.addTokensToTool(toolId, tokens, seller);
            redirectAttrs.addFlashAttribute("success", "Tokens added successfully.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/seller/tools";
    }

    /**
     * ✅ Xoá token (DB mode)
     */
    @PostMapping("/delete/{tokenId}")
    public String deleteToken(@PathVariable Long tokenId, RedirectAttributes redirectAttrs) {
        try {
            tokenService.deleteToken(tokenId);
            redirectAttrs.addFlashAttribute("success", "Token deleted successfully.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/seller/tools";
    }
}
