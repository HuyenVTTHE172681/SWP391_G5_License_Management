package swp391.fa25.lms.controller.seller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.Tool;
import swp391.fa25.lms.service.seller.TokenService;
import swp391.fa25.lms.service.seller.ToolFlowService;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/seller/token-manage")
@Validated
public class TokenController {

    @Autowired
    private ToolFlowService toolFlowService;

    @Autowired
    private TokenService tokenService;

    // ==========================================================
    // 🔹 FLOW 1: TOKEN FINALIZATION (KHI TẠO TOOL MỚI)
    // ==========================================================

    /**
     * ✅ Hiển thị trang nhập token sau khi seller chọn loginMethod = TOKEN.
     * - Dùng session "pendingTool"
     * - Nếu session không tồn tại → quay lại trang Add Tool
     */
    @GetMapping
    public String showTokenManage(HttpSession session, RedirectAttributes redirectAttrs) {
        var pendingTool = session.getAttribute("pendingTool");
        if (pendingTool == null) {
            redirectAttrs.addFlashAttribute("error", "No pending tool found. Please create a tool first.");
            return "redirect:/seller/tools/add";
        }
        return "seller/token-manage";
    }

    /**
     * ✅ Khi seller nhập danh sách token và bấm “Finalize Tool”
     * - Validate token format (6 số)
     * - Gọi ToolFlowService để finalize và lưu vào DB
     */
    @PostMapping("/submit")
    public String finalizeNewToolTokens(
            @RequestParam("tokens")
            List<@Pattern(regexp = "^[0-9]{6}$", message = "Each token must contain exactly 6 digits") String> tokens,
            HttpSession session,
            RedirectAttributes redirectAttrs) {

        try {
            var pendingTool = session.getAttribute("pendingTool");
            if (pendingTool == null) {
                redirectAttrs.addFlashAttribute("error", "Session expired. Please create the tool again.");
                return "redirect:/seller/tools/add";
            }

            toolFlowService.finalizeTokenTool(tokens, session);
            redirectAttrs.addFlashAttribute("success", "✅ Tool created successfully!");
            return "redirect:/seller/tools";

        } catch (IllegalArgumentException e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        } catch (IOException e) {
            redirectAttrs.addFlashAttribute("error", "File upload error: " + e.getMessage());
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "Unexpected error: " + e.getMessage());
        }
        return "redirect:/seller/token-manage";
    }

    /**
     * ✅ Khi seller bấm “Cancel” → xóa tool tạm trong session & quay lại form add tool
     */
    @PostMapping("/back")
    public String cancelNewToolCreation(HttpSession session, RedirectAttributes redirectAttrs) {
        toolFlowService.cancelToolCreation(session);
        redirectAttrs.addFlashAttribute("info", "Tool creation canceled. Returning to add form.");
        return "redirect:/seller/tools/add";
    }

    // ==========================================================
    // 🔹 FLOW: EDIT TOKEN (SESSION-ONLY)
    // ==========================================================

    /**
     * ✅ Hiển thị màn token-edit từ session
     */
    @GetMapping("/edit")
    public String showEditTokenManage(HttpSession session, Model model, RedirectAttributes redirectAttrs) {
        ToolFlowService.ToolSessionData pending =
                (ToolFlowService.ToolSessionData) session.getAttribute("pendingEditTool");

        if (pending == null || pending.getTool() == null) {
            redirectAttrs.addFlashAttribute("error", "No tool in edit session. Please start from edit form again.");
            return "redirect:/seller/tools";
        }

        Tool tool = pending.getTool();
        model.addAttribute("tool", tool);
        model.addAttribute("tokens", pending.getTokens());

        return "seller/token-edit";
    }

    /**
     * ✅ Add token vào session
     */
    @PostMapping("/edit/add")
    public String addTokenToSession(
            @RequestParam("token") @Pattern(regexp = "^\\d{6}$", message = "Token must be 6 digits") String token,
            HttpSession session,
            RedirectAttributes redirectAttrs) {

        ToolFlowService.ToolSessionData pending =
                (ToolFlowService.ToolSessionData) session.getAttribute("pendingEditTool");

        if (pending == null) {
            redirectAttrs.addFlashAttribute("error", "Session expired. Please reload edit form.");
            return "redirect:/seller/tools";
        }

        List<String> tokens = pending.getTokens();
        if (tokens.contains(token)) {
            redirectAttrs.addFlashAttribute("error", "Token already exists.");
        } else {
            tokens.add(token);
            redirectAttrs.addFlashAttribute("success", "Token added successfully (session only).");
        }

        return "redirect:/seller/token-manage/edit";
    }

    /**
     * ✅ Delete token khỏi session
     */
    @PostMapping("/edit/delete")
    public String deleteTokenFromSession(@RequestParam("token") String token,
                                         HttpSession session,
                                         RedirectAttributes redirectAttrs) {

        ToolFlowService.ToolSessionData pending =
                (ToolFlowService.ToolSessionData) session.getAttribute("pendingEditTool");

        if (pending == null) {
            redirectAttrs.addFlashAttribute("error", "Session expired. Please reload edit form.");
            return "redirect:/seller/tools";
        }

        List<String> tokens = pending.getTokens();
        if (tokens.remove(token)) {
            redirectAttrs.addFlashAttribute("success", "Token removed from session.");
        } else {
            redirectAttrs.addFlashAttribute("error", "Token not found.");
        }

        return "redirect:/seller/token-manage/edit";
    }

    /**
     * ✅ Finalize tokens (commit vào DB)
     */
    @PostMapping("/edit/finalize")
    public String finalizeEditTokens(@RequestParam("tokens") String tokenString,
                                     HttpSession session,
                                     RedirectAttributes redirectAttrs) {
        try {
            List<String> tokens = Arrays.stream(tokenString.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();

            toolFlowService.finalizeEditTokenTool(tokens, session);
            redirectAttrs.addFlashAttribute("success", "✅ Tokens and quantity updated successfully!");
            return "redirect:/seller/tools";
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
            return "redirect:/seller/token-manage/edit";
        }

    }

    /**
     * ✅ Back → hủy session edit
     */
    @PostMapping("/edit/back")
    public String handleBackFromEdit(HttpSession session, RedirectAttributes redirectAttrs) {
        toolFlowService.cancelToolCreation(session);
        redirectAttrs.addFlashAttribute("info", "Token edit canceled. Returning to tools.");
        return "redirect:/seller/tools";
    }
}
