package swp391.fa25.lms.controller.seller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.Tool;
import swp391.fa25.lms.service.seller.AccountService;
import swp391.fa25.lms.service.seller.FileStorageService;
import swp391.fa25.lms.service.seller.ToolFlowService;
import swp391.fa25.lms.service.seller.ToolService;

import java.io.IOException;
import java.util.*;

@Controller
@RequestMapping("/seller/tools")
public class ToolController {

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    @Qualifier("sellerToolService")
    private ToolService toolService;

    @Autowired
    private ToolFlowService toolFlowService;

    @Autowired
    @Qualifier("sellerAccountService")
    private AccountService accountService;

    // ==========================================================
    // 🔹 FLOW 1: TOOL LIST + MANAGEMENT
    // ==========================================================

    /**
     * ✅ Trang danh sách Tool của seller
     */
    @GetMapping
    public String showToolList(Model model, HttpSession session, RedirectAttributes redirectAttrs) {
        Account seller = (Account) session.getAttribute("loggedInAccount");
        if (seller == null) {
            redirectAttrs.addFlashAttribute("error", "Session expired. Please login again.");
            return "redirect:/login";
        }

        // ✅ Kiểm tra hạn sử dụng
        boolean isActive = accountService.isSellerActive(seller);
        model.addAttribute("sellerExpired", !isActive);

        // Nếu đã hết hạn, có thể cập nhật trạng thái DB (optional)
        if (!isActive && Boolean.TRUE.equals(seller.getSellerActive())) {
            seller.setSellerActive(false);
        }

        model.addAttribute("categories", toolService.getAllCategories());
        model.addAttribute("tools", toolService.getToolsBySeller(seller));

        return "seller/tool-list";
    }

    /**
     * ✅ Đổi trạng thái tool (VD: deactivate)
     */
    @PostMapping("/{id}/deactivate")
    public String deactivateTool(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttrs) {
        Account seller = (Account) session.getAttribute("loggedInAccount");
        if (seller == null) {
            redirectAttrs.addFlashAttribute("error", "Please login again.");
            return "redirect:/login";
        }
        try {
            toolService.deactivateTool(id);
            redirectAttrs.addFlashAttribute("success", "Tool has been deactivated.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/seller/tools";
    }

    /**
     * ✅ API lấy danh sách Tool của Seller (cho JS fetch)
     */
    @GetMapping("/api")
    @ResponseBody
    public ResponseEntity<?> getToolsApi(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String loginMethod,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size,
            HttpSession session
    ) {
        Account seller = (Account) session.getAttribute("loggedInAccount");
        if (seller == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not logged in");
        }

        Pageable pageable = switch (sort) {
            case "oldest" -> PageRequest.of(page, size, Sort.by("createdAt").ascending());
            case "price,asc" -> PageRequest.of(page, size, Sort.by("licenses.price").ascending());
            case "price,desc" -> PageRequest.of(page, size, Sort.by("licenses.price").descending());
            default -> PageRequest.of(page, size, Sort.by("createdAt").descending());
        };

        Page<Tool> tools = toolService.searchToolsForSeller(
                seller.getAccountId(),
                keyword,
                categoryId,
                status,
                loginMethod,
                minPrice,
                maxPrice,
                pageable
        );

        return ResponseEntity.ok(tools);
    }

    /**
     * ✅ API lấy danh mục (cho dropdown filter)
     */
    @GetMapping("/categories")
    @ResponseBody
    public ResponseEntity<?> getCategories() {
        return ResponseEntity.ok(toolService.getAllCategories());
    }

    // ==========================================================
    // 🔹 FLOW 2: TOOL CREATION
    // ==========================================================

    /**
     * ✅ Hiển thị form Add Tool
     */
    @GetMapping("/add")
    public String showAddToolForm(Model model, HttpSession session,RedirectAttributes redirectAttrs) {

        Account seller = (Account) session.getAttribute("loggedInAccount");
        if (seller == null) {
            redirectAttrs.addFlashAttribute("error", "Please login again.");
            return "redirect:/login";
        }

        ToolFlowService.ToolSessionData pending =
                (ToolFlowService.ToolSessionData) session.getAttribute("pendingTool");

        if (pending != null) {
            model.addAttribute("tool", pending.getTool());
            model.addAttribute("licenses", pending.getLicenses());
            model.addAttribute("categoryId", pending.getCategory().getCategoryId());
            model.addAttribute("restoreFromSession", true);
        } else {
            model.addAttribute("tool", new Tool());
        }

        model.addAttribute("categories", toolService.getAllCategories());
        return "seller/tool-add";
    }

    /**
     * ✅ Xử lý khi seller submit form "Add New Tool"
     */
    @PostMapping("/add")
    public String addTool(
            @Valid @ModelAttribute("tool") Tool tool,
            BindingResult result,
            @RequestParam("imageFile") MultipartFile imageFile,
            @RequestParam("toolFile") MultipartFile toolFile,
            @RequestParam("licenseDays") List<Integer> licenseDays,
            @RequestParam("licensePrices") List<Double> licensePrices,
            HttpSession session,
            RedirectAttributes redirectAttrs,
            Model model) {
        Account seller = (Account) session.getAttribute("loggedInAccount");
        if (seller == null) {
            redirectAttrs.addFlashAttribute("error", "Please login again.");
            return "redirect:/login";
        }

        if (result.hasErrors()) {
            model.addAttribute("categories", toolService.getAllCategories());
            return "seller/tool-add";
        }

        try {
            toolFlowService.startCreateTool(
                    tool, imageFile, toolFile,
                    tool.getCategory().getCategoryId(),
                    licenseDays, licensePrices, session
            );

            if (tool.getLoginMethod() == Tool.LoginMethod.TOKEN) {
                redirectAttrs.addFlashAttribute("info", "Please add tokens to finalize your tool.");
                return "redirect:/seller/token-manage";
            }

            redirectAttrs.addFlashAttribute("success", "Tool created successfully!");
            return "redirect:/seller/tools";

        } catch (IOException e) {
            redirectAttrs.addFlashAttribute("error", "File upload error: " + e.getMessage());
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/seller/tools/add";
    }

    // ==========================================================
    // 🔹 FLOW 3: TOOL EDIT
    // ==========================================================

    /**
     * ✅ Hiển thị form Edit Tool
     */
    @GetMapping("/edit/{id}")
    public String showEditToolForm(
            @PathVariable Long id,
            Model model,
            HttpSession session,
            RedirectAttributes redirectAttrs) {

        Account seller = (Account) session.getAttribute("loggedInAccount");
        if (seller == null) {
            redirectAttrs.addFlashAttribute("error", "Please login again.");
            return "redirect:/login";
        }

        Tool tool = toolService.getToolByIdAndSeller(id, seller);
        if (tool == null) {
            redirectAttrs.addFlashAttribute("error", "Tool not found or unauthorized.");
            return "redirect:/seller/tools";
        }

        model.addAttribute("tool", tool);
        model.addAttribute("categories", toolService.getAllCategories());
        model.addAttribute("isEdit", true);
        model.addAttribute("isTokenLogin", tool.getLoginMethod() == Tool.LoginMethod.TOKEN);
        return "seller/tool-edit";
    }

    /**
     * ✅ Cập nhật Tool (User_Password hoặc Token Flow)
     */
    @PostMapping("/edit/{id}")
    public String updateTool(
            @PathVariable Long id,
            @ModelAttribute("tool") Tool updatedTool,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            @RequestParam(value = "toolFile", required = false) MultipartFile toolFile,
            @RequestParam(value = "licenseDays", required = false) List<Integer> licenseDays,
            @RequestParam(value = "licensePrices", required = false) List<Double> licensePrices,
            @RequestParam(value = "action", required = false) String action,
            HttpSession session,
            RedirectAttributes redirectAttrs) {

        Account seller = (Account) session.getAttribute("loggedInAccount");
        if (seller == null) {
            redirectAttrs.addFlashAttribute("error", "Please login again.");
            return "redirect:/login";
        }

        try {
            Tool existing = toolService.getToolByIdAndSeller(id, seller);
            if (existing == null) {
                throw new IllegalArgumentException("Tool not found or unauthorized.");
            }

            String imagePath = null;
            String toolPath = null;

            if (imageFile != null && !imageFile.isEmpty()) {
                imagePath = fileStorageService.uploadImage(imageFile);
            }
            if (toolFile != null && !toolFile.isEmpty()) {
                toolPath = fileStorageService.uploadToolFile(toolFile);
            }

            // ✅ TOKEN → redirect sang token-edit flow
            if ("token".equals(action) && existing.getLoginMethod() == Tool.LoginMethod.TOKEN) {
                List<Integer> days = (licenseDays != null) ? licenseDays : new ArrayList<>();
                List<Double> prices = (licensePrices != null) ? licensePrices : new ArrayList<>();

                toolFlowService.startEditToolSession(existing, updatedTool, imageFile, toolFile, days, prices, session);
                redirectAttrs.addFlashAttribute("info", "Please review and update tokens for this tool.");
                return "redirect:/seller/token-manage/edit";
            }

            // ✅ USER_PASSWORD → cập nhật trực tiếp
            toolService.updateTool(
                    id,
                    updatedTool,
                    imagePath,
                    toolPath,
                    licenseDays != null ? licenseDays : new ArrayList<>(),
                    licensePrices != null ? licensePrices : new ArrayList<>(),
                    seller
            );

            redirectAttrs.addFlashAttribute("success", "Tool updated successfully!");
            return "redirect:/seller/tools";

        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
            return "redirect:/seller/tools/edit/" + id;
        }
    }
}
