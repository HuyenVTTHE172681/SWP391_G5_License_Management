package swp391.fa25.lms.controller.seller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.beans.factory.annotation.Autowired;
import swp391.fa25.lms.model.*;
import swp391.fa25.lms.repository.*;
import swp391.fa25.lms.service.seller.CategoryService;
import swp391.fa25.lms.service.seller.ToolService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/seller/tools")
public class ToolController {

    @Autowired private CategoryRepository categoryRepo;
    @Autowired @Qualifier("seller1") private CategoryService categoryService;
    @Autowired @Qualifier("seller") private ToolService toolService;
    @Autowired private LicenseToolRepository licenseRepo;

    // ✅ Hàm tiện ích: lấy seller từ session
    private Account getCurrentSeller(HttpServletRequest request) {
        Account account = (Account) request.getSession().getAttribute("loggedInAccount");

        if (account == null) {
            throw new RuntimeException("Bạn chưa đăng nhập");
        }

        if (account.getRole() == null || account.getRole().getRoleName() != Role.RoleName.SELLER) {
            throw new RuntimeException("Tài khoản không phải là seller");
        }

        return account;
    }

    @InitBinder("tool")
    public void disallow(WebDataBinder b) {
        b.setDisallowedFields("createdAt", "updatedAt", "status", "image", "seller", "files", "licenses");
    }

    @GetMapping
    public String listTools(HttpServletRequest request,
                            Model model,
                            @RequestParam(required = false) String keyword,
                            @RequestParam(required = false) Long categoryId,
                            @RequestParam(required = false) Double min,
                            @RequestParam(required = false) Double max,
                            @RequestParam(required = false, defaultValue = "newest") String sort) {

        Account seller = getCurrentSeller(request);
        List<Tool> tools = toolService.getToolsBySeller(seller);

        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.toLowerCase();
            tools = tools.stream()
                    .filter(t -> t.getToolName().toLowerCase().contains(kw))
                    .collect(Collectors.toList());
        }

        if (categoryId != null && categoryId > 0) {
            tools = tools.stream()
                    .filter(t -> t.getCategory() != null && t.getCategory().getCategoryId().equals(categoryId))
                    .collect(Collectors.toList());
        }

        if (min != null || max != null) {
            double minVal = (min != null) ? min : 0;
            double maxVal = (max != null) ? max : Double.MAX_VALUE;

            tools = tools.stream().filter(t -> {
                var licenses = licenseRepo.findByToolToolId(t.getToolId());
                double lowestPrice = licenses.stream()
                        .mapToDouble(License::getPrice)
                        .min().orElse(Double.MAX_VALUE);
                return lowestPrice >= minVal && lowestPrice <= maxVal;
            }).collect(Collectors.toList());
        }

        switch (sort.toLowerCase()) {
            case "price_asc":
                tools.sort(Comparator.comparingDouble(t ->
                        licenseRepo.findByToolToolId(t.getToolId())
                                .stream().mapToDouble(License::getPrice).min().orElse(Double.MAX_VALUE)));
                break;
            case "price_desc":
                tools.sort(Comparator.comparingDouble((Tool t) ->
                        licenseRepo.findByToolToolId(t.getToolId())
                                .stream().mapToDouble(License::getPrice).min().orElse(Double.MAX_VALUE)).reversed());
                break;
            default:
                tools.sort(Comparator.comparing(Tool::getCreatedAt).reversed());
                break;
        }

        model.addAttribute("tools", tools);
        model.addAttribute("categories", categoryRepo.findAll());
        model.addAttribute("keyword", keyword);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("min", min);
        model.addAttribute("max", max);
        model.addAttribute("sort", sort);

        return "seller/tool-list";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) throws IOException {
        model.addAttribute("tool", new Tool());
        model.addAttribute("categories", categoryService.getAllCategories());

        Path imageDir = Path.of(new ClassPathResource("static/images").getURI());
        List<String> imageFiles = Files.list(imageDir)
                .filter(Files::isRegularFile)
                .map(path -> path.getFileName().toString())
                .collect(Collectors.toList());

        model.addAttribute("imageFiles", imageFiles);
        return "seller/tool-add";
    }

    @PostMapping("/add")
    public String add(HttpServletRequest request,
                      @ModelAttribute("tool") Tool tool,
                      @RequestParam(value = "selectedImage", required = false) String selectedImage,
                      @RequestParam(value = "licenseDays", required = false) List<Integer> licenseDays,
                      @RequestParam(value = "licensePrices", required = false) List<Double> licensePrices,
                      @RequestParam(value = "loginMethods", required = false) List<String> loginMethods,
                      @RequestParam(value = "redirectTokenPage", required = false) Boolean redirectTokenPage,
                      RedirectAttributes redirectAttributes) throws Exception {

        Account seller = getCurrentSeller(request);

        if (tool.getCategory() != null && tool.getCategory().getCategoryId() != null) {
            categoryRepo.findById(tool.getCategory().getCategoryId()).ifPresent(tool::setCategory);
        } else {
            tool.setCategory(null);
        }

        if (selectedImage != null && !selectedImage.isBlank()) {
            tool.setImage(selectedImage);
        }

        tool.setCreatedAt(LocalDateTime.now());
        tool.setUpdatedAt(LocalDateTime.now());
        tool.setStatus(Tool.Status.PENDING);

        if (loginMethods != null && !loginMethods.isEmpty()) {
            tool.setLoginMethods(new HashSet<>(loginMethods));
        } else {
            tool.setLoginMethods(new HashSet<>());
        }

        Tool savedTool = toolService.addTool(tool, seller);

        if (licenseDays != null && licensePrices != null) {
            for (int i = 0; i < licenseDays.size(); i++) {
                Integer days = licenseDays.get(i);
                Double price = licensePrices.get(i);
                if (days == null || price == null || days <= 0 || price < 0) continue;

                License license = new License();
                license.setTool(savedTool);
                license.setDurationDays(days);
                license.setPrice(price);
                license.setName("License " + days + " days");
                license.setCreatedAt(LocalDateTime.now());
                licenseRepo.save(license);
            }
        }

        if (tool.getLoginMethods().contains("TOKEN") || Boolean.TRUE.equals(redirectTokenPage)) {
            redirectAttributes.addAttribute("toolId", savedTool.getToolId());
            return "redirect:/seller/tokens/manage";
        }

        redirectAttributes.addFlashAttribute("message", "Thêm tool thành công!");
        return "redirect:/seller/tools";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) throws Exception {
        Tool tool = toolService.getToolById(id);

        Path imageDir = Path.of(new ClassPathResource("static/images").getURI());
        List<String> imageFiles = Files.list(imageDir)
                .filter(Files::isRegularFile)
                .map(path -> path.getFileName().toString())
                .collect(Collectors.toList());

        model.addAttribute("tool", tool);
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("imageFiles", imageFiles);
        model.addAttribute("statuses", Tool.Status.values());
        return "seller/tool-edit";
    }

    @Transactional
    @PostMapping("/edit/{id}")
    public String updateTool(@PathVariable Long id,
                             HttpServletRequest request,
                             @ModelAttribute Tool tool,
                             @RequestParam(value = "selectedImage", required = false) String selectedImage,
                             @RequestParam(value = "licenseDays", required = false) List<Integer> licenseDays,
                             @RequestParam(value = "licensePrices", required = false) List<Double> licensePrices,
                             RedirectAttributes redirectAttributes) throws Exception {

        try {
            Account seller = getCurrentSeller(request);
            Tool existingTool = toolService.getToolById(id);
            if (existingTool == null) throw new RuntimeException("Tool not found");

            if (selectedImage != null && !selectedImage.isBlank()) {
                existingTool.setImage(selectedImage);
            }

            if (tool.getCategory() != null && tool.getCategory().getCategoryId() != null) {
                categoryRepo.findById(tool.getCategory().getCategoryId()).ifPresent(existingTool::setCategory);
            }

            existingTool.setToolName(tool.getToolName());
            existingTool.setDescription(tool.getDescription());
            existingTool.setStatus(tool.getStatus());
            existingTool.setQuantity(tool.getQuantity());
            existingTool.setUpdatedAt(LocalDateTime.now());

            toolService.updateTool(id, existingTool, seller);

            if (licenseDays != null && licensePrices != null) {
                for (int i = 0; i < licenseDays.size(); i++) {
                    Integer days = licenseDays.get(i);
                    Double price = licensePrices.get(i);
                    if (days == null || price == null || days <= 0 || price < 0) continue;

                    License newLicense = new License();
                    newLicense.setTool(existingTool);
                    newLicense.setDurationDays(days);
                    newLicense.setPrice(price);
                    newLicense.setName("License " + days + " days");
                    newLicense.setCreatedAt(LocalDateTime.now());
                    licenseRepo.save(newLicense);
                }
            }

            redirectAttributes.addFlashAttribute("success", "Tool updated successfully!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/seller/tools/edit/" + id;
    }

    @PostMapping("/toggle/{id}")
    public String toggleToolStatus(@PathVariable Long id, HttpServletRequest request) {
        Account seller = getCurrentSeller(request);
        toolService.toggleToolStatus(id, seller);
        return "redirect:/seller/tools";
    }

    @GetMapping("/api")
    @ResponseBody
    public List<Tool> getToolsJson(HttpServletRequest request,
                                   @RequestParam(required = false) String q,
                                   @RequestParam(required = false) Long categoryId,
                                   @RequestParam(required = false) Double minPrice,
                                   @RequestParam(required = false) Double maxPrice,
                                   @RequestParam(required = false, defaultValue = "newest") String sort) {
        Account seller = getCurrentSeller(request);
        List<Tool> tools = toolService.getToolsBySeller(seller);

        if (q != null && !q.isBlank()) {
            tools = tools.stream()
                    .filter(t -> t.getToolName().toLowerCase().contains(q.toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (categoryId != null) {
            tools = tools.stream()
                    .filter(t -> t.getCategory() != null && t.getCategory().getCategoryId().equals(categoryId))
                    .collect(Collectors.toList());
        }

        if (minPrice != null || maxPrice != null) {
            double min = (minPrice != null) ? minPrice : 0;
            double max = (maxPrice != null) ? maxPrice : Double.MAX_VALUE;

            tools = tools.stream()
                    .filter(t -> licenseRepo.findByToolToolId(t.getToolId())
                            .stream().anyMatch(l -> l.getPrice() >= min && l.getPrice() <= max))
                    .collect(Collectors.toList());
        }

        Comparator<Tool> byCreated = Comparator.comparing(Tool::getCreatedAt).reversed();
        Comparator<Tool> byPrice = Comparator.comparingDouble(t ->
                licenseRepo.findByToolToolId(t.getToolId())
                        .stream().mapToDouble(License::getPrice)
                        .min().orElse(Double.MAX_VALUE));

        if ("price_asc".equalsIgnoreCase(sort)) tools.sort(byPrice);
        else if ("price_desc".equalsIgnoreCase(sort)) tools.sort(byPrice.reversed());
        else tools.sort(byCreated);

        return tools;
    }
}
