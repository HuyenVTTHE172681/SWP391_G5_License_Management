package swp391.fa25.lms.controller.seller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import swp391.fa25.lms.model.Tool;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.repository.CategoryRepo;
import swp391.fa25.lms.repository.ToolFileRepo;
import swp391.fa25.lms.service.CategoryService;
import swp391.fa25.lms.service.ToolService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/seller/tools")
public class ToolController {
    @Autowired
    private CategoryRepo categoryRepo;

    @Autowired
    private CategoryService categoryService;


    @Autowired
    private ToolService toolService;

    @Autowired
    private ToolFileRepo toolFileRepo;

    @InitBinder("tool")
    public void disallow(WebDataBinder b) {
        b.setDisallowedFields("createdAt", "updatedAt", "status", "image", "seller", "files", "licenses");
    }

    @GetMapping
    public String listTools(Model model, Principal principal) {
        Account seller = getCurrentSeller(principal);
        List<Tool> tools = toolService.getToolsBySeller(seller);
        model.addAttribute("tools", tools);
        return "seller/tool-list";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) throws IOException {
        model.addAttribute("tool", new Tool());
        model.addAttribute("categories", categoryService.getAllCategories());
        // ✅ Load danh sách ảnh có sẵn trong static/images
        Path imageDir = Path.of(new ClassPathResource("static/images").getURI());
        List<String> imageFiles = Files.list(imageDir)
                .filter(Files::isRegularFile)
                .map(path -> path.getFileName().toString())
                .collect(Collectors.toList());

        model.addAttribute("imageFiles", imageFiles);
        return "seller/tool-add";
    }

    @PostMapping("/add")
    public String add(@ModelAttribute("tool") Tool tool,
                      @RequestParam(value = "selectedImage", required = false) String selectedImage,
                      Principal principal) throws Exception {

        // ✅ Xử lý category
        if (tool.getCategory() != null && tool.getCategory().getCategoryId() != null) {
            Long catId = tool.getCategory().getCategoryId();
            categoryRepo.findById(catId).ifPresent(tool::setCategory);
        } else {
            tool.setCategory(null);
        }

        // ✅ Giả lập seller (tạm thời)
        Account seller = getCurrentSeller(principal);

        // ✅ Nếu user chọn ảnh có sẵn, gán vào tool
        if (selectedImage != null && !selectedImage.isBlank()) {
            tool.setImage(selectedImage);
        }

        // ✅ Thiết lập các giá trị mặc định
        tool.setCreatedAt(LocalDateTime.now());
        tool.setUpdatedAt(LocalDateTime.now());
        tool.setStatus(Tool.Status.PENDING);

        // ✅ Lưu tool vào database
        toolService.addTool(tool, seller);

        // ✅ Quay lại danh sách tool
        return "redirect:/seller/tools";
    }


    // TODO: thay bằng user thực tế khi có login
    private Account getCurrentSeller(Principal principal) {
        Account a = new Account();
        a.setAccountId(1L);
        return a;
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) throws Exception {
        Tool tool = toolService.getToolById(id);

        // ✅ Lấy danh sách ảnh trong static/imagine
        Path imageDir = Path.of(new ClassPathResource("static/images").getURI());
        List<String> imageFiles = Files.list(imageDir)
                .filter(Files::isRegularFile)
                .map(path -> path.getFileName().toString())
                .collect(Collectors.toList());


        model.addAttribute("tool", tool);
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("imageFiles", imageFiles);
        return "seller/tool-edit";
    }


    @PostMapping("/edit/{id}")
    public String updateTool(
            @PathVariable Long id,
            @ModelAttribute Tool tool,
            @RequestParam(value = "selectedImage", required = false) String selectedImage,
            Principal principal) throws Exception {

        Tool existingTool = toolService.getToolById(id);
        if (existingTool == null) {
            throw new RuntimeException("Tool not found");
        }

        Account seller = getCurrentSeller(principal);
        existingTool.setToolName(tool.getToolName());
        existingTool.setDescription(tool.getDescription());
        existingTool.setStatus(tool.getStatus());
        existingTool.setUpdatedAt(tool.getUpdatedAt() != null ? tool.getUpdatedAt() : java.time.LocalDateTime.now());

        if (tool.getCategory() != null && tool.getCategory().getCategoryId() != null) {
            Long catId = tool.getCategory().getCategoryId();
            categoryRepo.findById(catId).ifPresent(existingTool::setCategory);
        }

        // ✅ Đây là phần quan trọng: nếu user chọn ảnh mới, cập nhật lại đường dẫn
        if (selectedImage != null && !selectedImage.isBlank()) {
            existingTool.setImage(selectedImage);
        }

        toolService.save(existingTool);

        return "redirect:/seller/tools";
    }


    @GetMapping("/delete/{id}")
    public String deleteTool(@PathVariable Long id, Principal principal) {
        Account seller = getCurrentSeller(principal);
        toolService.deleteTool(id, seller);
        return "redirect:/seller/tools";
    }
}
