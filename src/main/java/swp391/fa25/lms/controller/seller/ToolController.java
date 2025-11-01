package swp391.fa25.lms.controller.seller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.Tool;
import swp391.fa25.lms.service.seller.ToolFlowService;
import swp391.fa25.lms.service.seller.ToolService;

import java.io.IOException;
import java.util.List;

@Controller
@RequestMapping("/seller/tools")
public class ToolController {

    @Autowired @Qualifier("sellerToolService") private ToolService toolService;
    @Autowired private ToolFlowService toolFlowService;

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
        model.addAttribute("categories", toolService.getAllCategories());


        model.addAttribute("tools", toolService.getToolsBySeller(seller));
        return "seller/tool-list";
    }

    /**
     * ✅ Trang thêm tool mới
     */
    @GetMapping("/add")
    public String showAddToolForm(Model model) {
        model.addAttribute("tool", new Tool());
        model.addAttribute("categories", toolService.getAllCategories());
        return "seller/tool-add";
    }

    /**
     * ✅ Xử lý khi người dùng gửi form "Create New Tool"
     */
    @PostMapping("/add")
    public String addTool(@Valid @ModelAttribute("tool") Tool tool,
                          BindingResult result,
                          @RequestParam("imageFile") MultipartFile imageFile,
                          @RequestParam("toolFile") MultipartFile toolFile,
                          @RequestParam("licenseDays") List<Integer> licenseDays,
                          @RequestParam("licensePrices") List<Double> licensePrices,
                          HttpSession session,
                          RedirectAttributes redirectAttrs,
                          Model model) {

        if (result.hasErrors()) {
            model.addAttribute("categories", toolService.getAllCategories());
            return "seller/tool-add";
        }

        try {
            toolFlowService.startCreateTool(
                    tool, imageFile, toolFile,tool.getCategory().getCategoryId(),
                    licenseDays, licensePrices, session
            );

            // Nếu chọn TOKEN → chuyển sang token-manage
            if (tool.getLoginMethod() == Tool.LoginMethod.TOKEN) {
                redirectAttrs.addFlashAttribute("info", "Please add tokens to finalize your tool.");
                return "redirect:/seller/token-manage";
            }

            redirectAttrs.addFlashAttribute("success", "Tool created successfully!");
            return "redirect:/seller/tools";

        } catch (IOException e) {
            redirectAttrs.addFlashAttribute("error", "File upload error: " + e.getMessage());
            return "redirect:/seller/tools/add";
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
            return "redirect:/seller/tools/add";
        }
    }

    /**
     * ✅ Đổi trạng thái tool (VD: deactivate)
     */
    @PostMapping("/{id}/deactivate")
    public String deactivateTool(@PathVariable Long id, RedirectAttributes redirectAttrs) {
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

        Pageable pageable;
        switch (sort) {
            case "oldest":
                pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending());
                break;
            case "price,asc":
                pageable = PageRequest.of(page, size, Sort.by("licenses.price").ascending());
                break;
            case "price,desc":
                pageable = PageRequest.of(page, size, Sort.by("licenses.price").descending());
                break;
            default:
                pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        }

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
    // ✅ Thêm API lấy category ở đây
    @GetMapping("/categories")
    @ResponseBody
    public ResponseEntity<?> getCategories() {
        return ResponseEntity.ok(toolService.getAllCategories());
    }
}


