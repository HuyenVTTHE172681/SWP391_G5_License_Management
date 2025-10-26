package swp391.fa25.lms.controller.seller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import swp391.fa25.lms.model.*;
import swp391.fa25.lms.repository.CategoryRepository;
import swp391.fa25.lms.repository.LicenseToolRepository;
import swp391.fa25.lms.repository.ToolFileRepository;
import swp391.fa25.lms.service.seller.CategoryService;
import swp391.fa25.lms.service.seller.ToolService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Controller
@RequestMapping("/seller/tools")
public class ToolController {

    @Autowired private CategoryRepository categoryRepo;
    @Autowired @Qualifier("seller1") private CategoryService categoryService;
    @Autowired @Qualifier("seller") private ToolService toolService;
    @Autowired private LicenseToolRepository licenseRepo;
    @Autowired private ToolFileRepository toolFileRepository;

    // ================== COMMON ==================

    private Account getCurrentSeller(HttpServletRequest request) {
        Account account = (Account) request.getSession().getAttribute("loggedInAccount");
        if (account == null) throw new RuntimeException("Bạn chưa đăng nhập");
        if (account.getRole() == null || account.getRole().getRoleName() != Role.RoleName.SELLER)
            throw new RuntimeException("Tài khoản không phải là seller");
        return account;
    }

    @InitBinder("tool")
    public void disallow(WebDataBinder binder) {
        binder.setDisallowedFields("createdAt", "updatedAt", "status", "seller",
                "files", "licenses", "selectedImage", "licenseDays", "licensePrices");
    }

    private List<String> getToolImageFiles() {
        try {
            Path imageDir = Path.of(new ClassPathResource("static/images/tools").getURI());
            if (!Files.exists(imageDir)) return List.of();

            try (Stream<Path> paths = Files.list(imageDir)) {
                return paths.filter(Files::isRegularFile)
                        .map(p -> p.getFileName().toString())
                        .collect(Collectors.toList());
            }
        } catch (IOException e) {
            e.printStackTrace();
            return List.of();
        }
    }

    private void reloadFormData(Model model) throws IOException {
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("imageFiles", getToolImageFiles());
    }

    // ================== LIST TOOLS ==================

    @GetMapping
    public String listTools(HttpServletRequest request, Model model,
                            @RequestParam(required = false) String keyword,
                            @RequestParam(required = false) Long categoryId,
                            @RequestParam(required = false) Double min,
                            @RequestParam(required = false) Double max,
                            @RequestParam(required = false, defaultValue = "newest") String sort) {

        Account seller = getCurrentSeller(request);
        List<Tool> tools = toolService.getToolsBySeller(seller);

        if (keyword != null && !keyword.isBlank()) {
            tools = tools.stream()
                    .filter(t -> t.getToolName().toLowerCase().contains(keyword.toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (categoryId != null && categoryId > 0) {
            tools = tools.stream()
                    .filter(t -> t.getCategory() != null &&
                            t.getCategory().getCategoryId().equals(categoryId))
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
            case "price_asc" -> tools.sort(Comparator.comparingDouble(t ->
                    licenseRepo.findByToolToolId(t.getToolId())
                            .stream().mapToDouble(License::getPrice)
                            .min().orElse(Double.MAX_VALUE)));
            case "price_desc" -> tools.sort(Comparator.comparingDouble((Tool t) ->
                    licenseRepo.findByToolToolId(t.getToolId())
                            .stream().mapToDouble(License::getPrice)
                            .min().orElse(Double.MAX_VALUE)).reversed());
            default -> tools.sort(Comparator.comparing(Tool::getCreatedAt).reversed());
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

    // ================== ADD TOOL ==================

    @GetMapping("/add")
    public String showAddForm(Model model) throws IOException {
        model.addAttribute("tool", new Tool());
        reloadFormData(model);
        return "seller/tool-add";
    }

    @PostMapping("/add")
    @Transactional
    public String addTool(HttpServletRequest request,
                          @Valid @ModelAttribute("tool") Tool tool,
                          BindingResult bindingResult,
                          @RequestParam(value = "loginMethod", required = false) String loginMethod,
                          @RequestParam(value = "licenseDays", required = false) List<Integer> licenseDays,
                          @RequestParam(value = "licensePrices", required = false) List<Double> licensePrices,
                          RedirectAttributes redirectAttributes,
                          Model model) throws Exception {

        if (tool.getImage() == null || tool.getImage().isBlank()) {
            model.addAttribute("errorImage", "Please select an image.");
            reloadFormData(model);
            return "seller/tool-add";
        }

        if (bindingResult.hasErrors()) {
            reloadFormData(model);
            return "seller/tool-add";
        }

        if (loginMethod == null || loginMethod.isBlank()) {
            model.addAttribute("errorLoginMethod", "Please select a login method.");
            reloadFormData(model);
            return "seller/tool-add";
        }
        if (tool.getCategory() == null || tool.getCategory().getCategoryId() == null) {
            model.addAttribute("errorCategory", "Please select a category.");
            reloadFormData(model);
            return "seller/tool-add";
        }
        HttpSession session = request.getSession();
        if ("TOKEN".equalsIgnoreCase(loginMethod)) {
            Category category = categoryRepo.findById(tool.getCategory().getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Invalid category selected"));
            tool.setCategory(category);
            // Lưu dữ liệu tạm vào session thay vì tạo tool luôn
            session.setAttribute("tempTool", tool);
            session.setAttribute("licenseDays", licenseDays);
            session.setAttribute("licensePrices", licensePrices);
            redirectAttributes.addFlashAttribute("message", "Please add token keys to finish setup.");
            return "redirect:/seller/tokens/manage";
        }

        Account seller = getCurrentSeller(request);
        Category category = categoryRepo.findById(tool.getCategory().getCategoryId())
                .orElseThrow(() -> new RuntimeException("Invalid category selected"));
        tool.setCategory(category);
        tool.setSeller(seller);
        tool.setStatus(Tool.Status.PENDING);
        tool.setLoginMethod(Tool.LoginMethod.valueOf(loginMethod));
        tool.setCreatedAt(LocalDateTime.now());
        tool.setUpdatedAt(LocalDateTime.now());

        Tool saved = toolService.addTool(tool, seller);

        if (licenseDays != null && licensePrices != null) {
            for (int i = 0; i < Math.min(licenseDays.size(), licensePrices.size()); i++) {
                Integer d = licenseDays.get(i);
                Double p = licensePrices.get(i);
                if (d == null || p == null || d <= 0 || p < 0) continue;

                License l = new License();
                l.setTool(saved);
                l.setDurationDays(d);
                l.setPrice(p);
                l.setName("License " + d + " days");
                l.setCreatedAt(LocalDateTime.now());
                licenseRepo.save(l);
            }
        }
        redirectAttributes.addFlashAttribute("success", "Tool added successfully!");
        return "redirect:/seller/tools";
    }

    // ================== EDIT TOOL ==================

    @GetMapping("/edit/{id}")
    public String showEditToolForm(@PathVariable("id") Long id,
                                   HttpServletRequest request,
                                   Model model) throws IOException {
        Account seller = getCurrentSeller(request);
        Tool tool = toolService.getToolById(id);
        if (!tool.getSeller().getAccountId().equals(seller.getAccountId())) {
            throw new RuntimeException("You are not allowed to edit this tool");
        }

        model.addAttribute("tool", tool);
        reloadFormData(model);
        return "seller/tool-edit";
    }

    @PostMapping("/edit/{id}")
    @Transactional
    public String updateTool(@PathVariable Long id,
                             HttpServletRequest request,
                             @ModelAttribute Tool tool,
                             @RequestParam(value = "selectedImage", required = false) String selectedImage,
                             @RequestParam(value = "loginMethod", required = false) String loginMethod,
                             @RequestParam(value = "licenseDays", required = false) List<Integer> licenseDays,
                             @RequestParam(value = "licensePrices", required = false) List<Double> licensePrices,
                             RedirectAttributes redirectAttributes) {

        try {
            Account seller = getCurrentSeller(request);
            Tool existingTool = toolService.getToolById(id);
            if (existingTool == null) throw new RuntimeException("Tool not found");

            if (!existingTool.getSeller().getAccountId().equals(seller.getAccountId())) {
                throw new RuntimeException("You are not allowed to edit this tool");
            }

            if (selectedImage != null && !selectedImage.isBlank()) {
                existingTool.setImage(selectedImage);
            }

            if (tool.getCategory() != null && tool.getCategory().getCategoryId() != null) {
                categoryRepo.findById(tool.getCategory().getCategoryId())
                        .ifPresent(existingTool::setCategory);
            }

            if (loginMethod != null && !loginMethod.isBlank()) {
                existingTool.setLoginMethod(Tool.LoginMethod.valueOf(loginMethod));
            }

            existingTool.setToolName(tool.getToolName());
            existingTool.setDescription(tool.getDescription());
            existingTool.setQuantity(tool.getQuantity());
            existingTool.setStatus(Tool.Status.PENDING);
            existingTool.setUpdatedAt(LocalDateTime.now());

            toolService.save(existingTool);

            // Xóa license cũ và thêm mới
            if (licenseDays != null && licensePrices != null) {
                licenseRepo.deleteAll(licenseRepo.findByToolToolId(existingTool.getToolId()));
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

            redirectAttributes.addFlashAttribute("successMessage", "✅ Tool updated successfully!");
            return "redirect:/seller/tools";

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "❌ Error: " + e.getMessage());
            return "redirect:/seller/tools/edit/" + id;
        }
    }

    // ================== TOGGLE TOOL STATUS ==================

    @PostMapping("/toggle/{id}")
    public String toggleToolStatus(@PathVariable Long id, HttpServletRequest request) {
        Account seller = getCurrentSeller(request);
        toolService.toggleToolStatus(id, seller);
        return "redirect:/seller/tools";
    }

    // ================== API JSON ==================

    @GetMapping("/api")
    @ResponseBody
    public List<Tool> getToolsJson(HttpServletRequest request,
                                   @RequestParam(required = false) String q,
                                   @RequestParam(required = false) Long categoryId,
                                   @RequestParam(required = false) Double minPrice,
                                   @RequestParam(required = false) Double maxPrice,
                                   @RequestParam(required = false, defaultValue = "newest") String sort,
                                   @RequestParam(required = false) String status) {

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

        if (status != null && !status.isBlank()) {
            try {
                Tool.Status st = Tool.Status.valueOf(status.toUpperCase());
                tools = tools.stream()
                        .filter(t -> t.getStatus() == st)
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException ignored) {}
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
