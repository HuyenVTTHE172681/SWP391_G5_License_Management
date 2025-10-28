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
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import swp391.fa25.lms.model.*;
import swp391.fa25.lms.repository.CategoryRepository;
import swp391.fa25.lms.repository.LicenseToolRepository;
import swp391.fa25.lms.repository.ToolFileRepository;
import swp391.fa25.lms.service.seller.CategoryService;
import swp391.fa25.lms.service.seller.ToolService;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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
        if (account == null)    throw new RuntimeException("Bạn chưa đăng nhập");
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
        if (!model.containsAttribute("tool")) {
            model.addAttribute("tool", new Tool());
        }
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
                          @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                          RedirectAttributes redirectAttributes,
                          Model model) throws Exception {
        // ✅ Validate file upload
        if (imageFile == null || imageFile.isEmpty()) {
            model.addAttribute("errorImage", "Please upload an image.");
            reloadFormData(model);
            return "seller/tool-add";
        }

        // ✅ Check file type (only JPG, JPEG, PNG)
        String contentType = imageFile.getContentType();
        if (contentType == null || !contentType.matches("image/(jpeg|jpg|png)")) {
            model.addAttribute("errorImage", "Only JPG, JPEG, or PNG files are allowed.");
            reloadFormData(model);
            return "seller/tool-add";
        }

        // ✅ Check file size (≤ 2MB)
        if (imageFile.getSize() > 2 * 1024 * 1024) {
            model.addAttribute("errorImage", "Image size must be smaller than 2MB.");
            reloadFormData(model);
            return "seller/tool-add";
        }

        // ✅ Save image outside classpath (allow same name, overwrite)
        try {
            Path uploadPath = Paths.get("uploads/tools");
            if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);

            // Lấy đúng tên file, chặn path traversal
            String originalFileName = imageFile.getOriginalFilename();
            if (originalFileName == null || originalFileName.isBlank()) {
                model.addAttribute("errorImage", "Invalid file name.");
                reloadFormData(model);
                return "seller/tool-add";
            }

            // chỉ giữ phần tên (bỏ mọi thư mục nếu có) và loại bỏ kí tự / \
            String safeName = Paths.get(originalFileName).getFileName().toString()
                    .replaceAll("[\\\\/]+", "");

            Path filePath = uploadPath.resolve(safeName);

            // ✅ Ghi đè nếu trùng tên
            Files.copy(imageFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // ✅ Lưu đúng tên file thực vào DB (không thêm hậu tố)
            tool.setImage(safeName);

        } catch (IOException e) {
            e.printStackTrace();
            model.addAttribute("errorImage", "Error saving image. Please try again.");
            reloadFormData(model);
            return "seller/tool-add";
        }
        // ✅ Now check bindingResult after image is set
        if (bindingResult.hasErrors()) {
            System.out.println("❌ bindingResult has errors: " + bindingResult.getAllErrors());
            reloadFormData(model);
            return "seller/tool-add";
        }

        // ✅ Validate login method
        if (loginMethod == null || loginMethod.isBlank()) {
            model.addAttribute("errorLoginMethod", "Please select a login method.");
            reloadFormData(model);
            return "seller/tool-add";
        }

        // ✅ Validate category
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
                             @Valid @ModelAttribute("tool") Tool tool,
                             BindingResult bindingResult,
                             @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                             @RequestParam(value = "licenseDays", required = false) List<Integer> licenseDays,
                             @RequestParam(value = "licensePrices", required = false) List<Double> licensePrices,
                             RedirectAttributes redirectAttributes,
                             Model model) {

        try {
            Account seller = getCurrentSeller(request);
            Tool existingTool = toolService.getToolById(id);
            if (existingTool == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Tool not found.");
                return "redirect:/seller/tools";
            }

            // 🔒 Check quyền chỉnh sửa
            if (!existingTool.getSeller().getAccountId().equals(seller.getAccountId())) {
                redirectAttributes.addFlashAttribute("errorMessage", "You are not allowed to edit this tool.");
                return "redirect:/seller/tools";
            }

            // ⚠️ Validate thủ công giống addTool()
            if (bindingResult.hasErrors()) {
                System.out.println("❌ Binding errors: " + bindingResult.getAllErrors());
                reloadEditData(model, tool); // ⚡ đổi lại đây
                return "seller/tool-edit";
            }
            // ✅ Validate category
            if (tool.getCategory() == null || tool.getCategory().getCategoryId() == null) {
                model.addAttribute("errorCategory", "Please select a category.");
                reloadEditData(model, existingTool);
                return "seller/tool-edit";
            }

            // ✅ Handle image upload (nếu có upload ảnh mới)
            if (imageFile != null && !imageFile.isEmpty()) {
                String contentType = imageFile.getContentType();
                if (contentType == null || !contentType.matches("image/(jpeg|jpg|png)")) {
                    model.addAttribute("errorImage", "Only JPG, JPEG, or PNG files are allowed.");
                    reloadEditData(model, existingTool);
                    return "seller/tool-edit";
                }
                if (imageFile.getSize() > 2 * 1024 * 1024) {
                    model.addAttribute("errorImage", "Image size must be smaller than 2MB.");
                    reloadEditData(model, existingTool);
                    return "seller/tool-edit";
                }

                try {
                    Path uploadPath = Paths.get("uploads/tools/");
                    if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);

                    String originalFileName = StringUtils.cleanPath(imageFile.getOriginalFilename());
                    Path filePath = uploadPath.resolve(originalFileName);
                    int count = 1;
                    String newFileName = originalFileName;

                    while (Files.exists(filePath)) {
                        String baseName = FilenameUtils.getBaseName(originalFileName);
                        String extension = FilenameUtils.getExtension(originalFileName);
                        newFileName = baseName + "_" + count + "." + extension;
                        filePath = uploadPath.resolve(newFileName);
                        count++;
                    }

                    Files.copy(imageFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                    existingTool.setImage(newFileName);

                } catch (IOException e) {
                    e.printStackTrace();
                    model.addAttribute("errorImage", "Error saving image. Please try again.");
                    reloadEditData(model, existingTool);
                    return "seller/tool-edit";
                }
            }

            // 🧩 Nếu không upload ảnh mới → giữ ảnh cũ
            else if (existingTool.getImage() == null || existingTool.getImage().isBlank()) {
                model.addAttribute("errorImage", "Please select an image.");
                reloadEditData(model, existingTool);
                return "seller/tool-edit";
            }

            // ✅ Cập nhật dữ liệu chính
            existingTool.setToolName(tool.getToolName());
            existingTool.setDescription(tool.getDescription());
            existingTool.setQuantity(tool.getQuantity());
            existingTool.setStatus(Tool.Status.PENDING);
            existingTool.setUpdatedAt(LocalDateTime.now());

            // Không cho đổi login method
            existingTool.setLoginMethod(existingTool.getLoginMethod());

            Category category = categoryRepo.findById(tool.getCategory().getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Invalid category selected"));
            existingTool.setCategory(category);

            toolService.save(existingTool);

            // ✅ Cập nhật license
            if (licenseDays != null && licensePrices != null) {
                licenseRepo.deleteAll(licenseRepo.findByToolToolId(existingTool.getToolId()));
                for (int i = 0; i < Math.min(licenseDays.size(), licensePrices.size()); i++) {
                    Integer d = licenseDays.get(i);
                    Double p = licensePrices.get(i);
                    if (d == null || p == null || d <= 0 || p < 0) continue;

                    License l = new License();
                    l.setTool(existingTool);
                    l.setDurationDays(d);
                    l.setPrice(p);
                    l.setName("License " + d + " days");
                    l.setCreatedAt(LocalDateTime.now());
                    licenseRepo.save(l);
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
    private void reloadEditData(Model model, Tool tool) {
        // 🔹 Lấy tool gốc từ DB để giữ lại dữ liệu không bị mất (ảnh, seller, loginMethod, v.v.)
        Tool existingTool = toolService.getToolById(tool.getToolId());
        if (existingTool != null) {
            // Nếu người dùng chưa upload ảnh mới → giữ ảnh cũ
            if (tool.getImage() == null || tool.getImage().isBlank()) {
                tool.setImage(existingTool.getImage());
            }

            // Giữ lại loginMethod (edit không được phép sửa)
            tool.setLoginMethod(existingTool.getLoginMethod());

            // Giữ lại seller (tránh null khi re-render)
            tool.setSeller(existingTool.getSeller());

            // Giữ lại licenses nếu form chưa có
            if (tool.getLicenses() == null || tool.getLicenses().isEmpty()) {
                tool.setLicenses(existingTool.getLicenses());
            }
        }

        // 🔹 Nạp lại categories
        model.addAttribute("categories", categoryRepo.findAll());

        // 🔹 Nạp lại danh sách ảnh có sẵn (nếu bạn hiển thị để chọn)
        try {
            Path imageDir = Paths.get("src/main/resources/static/images/tools");
            List<String> imageFiles = Files.list(imageDir)
                    .filter(Files::isRegularFile)
                    .map(f -> f.getFileName().toString())
                    .toList();
            model.addAttribute("imageFiles", imageFiles);
        } catch (IOException e) {
            model.addAttribute("imageFiles", List.of());
        }

        // 🔹 Cuối cùng add lại chính tool hiện tại (đã merge dữ liệu cũ)
        model.addAttribute("tool", tool);
    }

}
