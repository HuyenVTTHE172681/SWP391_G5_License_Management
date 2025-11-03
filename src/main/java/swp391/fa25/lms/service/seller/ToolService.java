package swp391.fa25.lms.service.seller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.fa25.lms.model.*;
import swp391.fa25.lms.repository.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service("sellerToolService")
@Transactional
public class ToolService {

    @Autowired private ToolRepository toolRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private LicenseToolRepository licenseRepository;
    @Autowired private LicenseAccountRepository licenseAccountRepository;

    // ==========================================================
    // 🔹 CRUD TOOL CƠ BẢN
    // ==========================================================

    /** ✅ Tạo mới Tool (luôn ở trạng thái PENDING) */
    public Tool createTool(Tool tool, Category category) {
        tool.setCategory(category);
        tool.setStatus(Tool.Status.PENDING);
        tool.setCreatedAt(LocalDateTime.now());
        tool.setUpdatedAt(LocalDateTime.now());
        return toolRepository.save(tool);
    }

    /** ✅ Lấy Tool theo ID và Seller */
    public Tool getToolByIdAndSeller(Long id, Account seller) {
        return toolRepository.findByToolIdAndSeller(id, seller).orElse(null);
    }

    /** ✅ Lấy Tool theo ID */
    public Tool getToolById(Long id) {
        return toolRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tool not found."));
    }

    /** ✅ Lấy danh sách Tool của Seller (trừ DEACTIVE) */
    public List<Tool> getToolsBySeller(Account seller) {
        return toolRepository.findBySellerAndStatusNot(seller, Tool.Status.DEACTIVATED);
    }

    @Transactional
    public void deactivateTool(Long id) {
        Tool tool = getToolById(id);
        if (tool == null) {
            throw new IllegalArgumentException("Tool not found");
        }

        tool.setStatus(Tool.Status.DEACTIVATED);
        tool.setUpdatedAt(LocalDateTime.now());
        toolRepository.saveAndFlush(tool);
    }

    // ==========================================================
    // 🔹 UPDATE TOOL (KHI EDIT)
    // ==========================================================

    /**
     * ✅ Cập nhật Tool
     * - Nếu imagePath / toolPath null → giữ nguyên
     * - Cập nhật licenses, category, quantity an toàn
     */
    @Transactional
    public void updateTool(Long id,
                           Tool updatedTool,
                           String imagePath,
                           String toolPath,
                           List<Integer> licenseDays,
                           List<Double> licensePrices,
                           Account seller) throws IOException {

        Tool existingTool = toolRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tool not found."));

        // 1️⃣ Thông tin cơ bản
        existingTool.setToolName(updatedTool.getToolName());
        existingTool.setDescription(updatedTool.getDescription());
        existingTool.setNote(updatedTool.getNote());
        existingTool.setUpdatedAt(LocalDateTime.now());

        if (updatedTool.getQuantity() != null) {
            existingTool.setQuantity(updatedTool.getQuantity());
        }

        // 2️⃣ Category (lấy entity thật từ DB)
        if (updatedTool.getCategory() != null && updatedTool.getCategory().getCategoryId() != null) {
            Category realCategory = categoryRepository.findById(updatedTool.getCategory().getCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Category not found."));
            existingTool.setCategory(realCategory);
        }

        // 3️⃣ Cập nhật ảnh (nếu có)
        if (imagePath != null && !imagePath.isBlank()) {
            existingTool.setImage(imagePath);
        }

        // 4️⃣ Cập nhật file tool (nếu có)
        if (toolPath != null && !toolPath.isBlank()) {
            if (existingTool.getFiles() == null)
                existingTool.setFiles(new ArrayList<>());

            ToolFile fileEntity = new ToolFile();
            fileEntity.setTool(existingTool);
            fileEntity.setFilePath(toolPath);
            fileEntity.setFileType(ToolFile.FileType.ORIGINAL);
            fileEntity.setUploadedBy(seller);
            fileEntity.setCreatedAt(LocalDateTime.now());
            existingTool.getFiles().add(fileEntity);
        }

        // 5️⃣ Cập nhật licenses (nếu gửi lên)
        if (licenseDays != null && licensePrices != null && licenseDays.size() == licensePrices.size()) {
            if (existingTool.getLicenses() == null)
                existingTool.setLicenses(new ArrayList<>());
            else
                existingTool.getLicenses().clear();

            for (int i = 0; i < licenseDays.size(); i++) {
                License lic = new License();
                lic.setTool(existingTool);
                lic.setName("License " + licenseDays.get(i) + " days");
                lic.setDurationDays(licenseDays.get(i));
                lic.setPrice(licensePrices.get(i));
                lic.setCreatedAt(LocalDateTime.now());
                existingTool.getLicenses().add(lic);
            }
        }

        toolRepository.save(existingTool);
    }

    // ==========================================================
    // 🔹 CATEGORY & LICENSE HANDLERS
    // ==========================================================

    /** ✅ Lấy tất cả Category */
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    /** ✅ Tìm Category theo ID */
    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found."));
    }

    /** ✅ Tạo Licenses cho Tool */
    public void createLicensesForTool(Tool tool, List<License> licenses) {
        for (License license : licenses) {
            license.setTool(tool);
            license.setCreatedAt(LocalDateTime.now());
            licenseRepository.save(license);
        }
    }

    /** ✅ Tạo LicenseAccount khi Tool dùng Token */
    public void createLicenseAccountsForTool(Tool tool, List<String> tokens) {
        for (String token : tokens) {
            if (licenseAccountRepository.existsByToken(token)) {
                throw new IllegalArgumentException("Duplicate token detected: " + token);
            }

            LicenseAccount acc = new LicenseAccount();
            acc.setTool(tool);
            acc.setLoginMethod(LicenseAccount.LoginMethod.TOKEN);
            acc.setToken(token);
            acc.setStatus(LicenseAccount.Status.ACTIVE);
            acc.setCreatedAt(LocalDateTime.now());
            licenseAccountRepository.save(acc);
        }
    }

    // ==========================================================
    // 🔹 QUẢN LÝ TOOL NÂNG CAO
    // ==========================================================

    /** ✅ Đổi trạng thái Tool (VD: Admin duyệt) */
    public void changeToolStatus(Long id, Tool.Status status) {
        Tool tool = getToolById(id);
        tool.setStatus(status);
        tool.setUpdatedAt(LocalDateTime.now());
        toolRepository.save(tool);
    }

    /** ✅ Kiểm tra trùng tên Tool */
    public boolean existsByToolName(String name) {
        return toolRepository.existsByToolName(name);
    }

    /** ✅ Tìm kiếm Tool của Seller (lọc & phân trang) */
    public Page<Tool> searchToolsForSeller(
            Long sellerId,
            String keyword,
            Long categoryId,
            String status,
            String loginMethod,
            Double minPrice,
            Double maxPrice,
            Pageable pageable
    ) {
        Tool.Status statusEnum = null;
        if (status != null && !status.isBlank()) {
            try {
                statusEnum = Tool.Status.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                statusEnum = null;
            }
        }
        Tool.LoginMethod loginEnum = null;
        if (loginMethod != null && !loginMethod.isBlank()) {
            try {
                loginEnum = Tool.LoginMethod.valueOf(loginMethod);
            } catch (IllegalArgumentException ignored) {
                loginEnum = null;
            }
        }

        return toolRepository.searchToolsForSeller(
                sellerId, keyword, categoryId, statusEnum, loginEnum, minPrice, maxPrice, pageable
        );
    }

    /** ✅ Cập nhật Quantity + Licenses cùng lúc */
    @Transactional
    public void updateQuantityAndLicenses(Long toolId, int newQuantity, List<License> newLicenses) {
        Tool tool = toolRepository.findById(toolId)
                .orElseThrow(() -> new IllegalArgumentException("Tool not found with id: " + toolId));

        tool.setQuantity(newQuantity);
        tool.getLicenses().clear();

        for (License license : newLicenses) {
            license.setTool(tool);
            tool.getLicenses().add(license);
        }

        toolRepository.save(tool);
    }
}
