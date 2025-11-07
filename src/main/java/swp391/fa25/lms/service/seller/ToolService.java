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

    public Tool createTool(Tool tool, Category category) {
        tool.setCategory(category);
        tool.setStatus(Tool.Status.PENDING);
        tool.setCreatedAt(LocalDateTime.now());
        tool.setUpdatedAt(LocalDateTime.now());
        return toolRepository.save(tool);
    }

    public Tool getToolByIdAndSeller(Long id, Account seller) {
        return toolRepository.findByToolIdAndSeller(id, seller).orElse(null);
    }

    public Tool getToolById(Long id) {
        return toolRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tool not found."));
    }

    public List<Tool> getToolsBySeller(Account seller) {
        return toolRepository.findBySellerAndStatusNot(seller, Tool.Status.DEACTIVATED);
    }

    @Transactional
    public void deactivateTool(Long id) {
        Tool tool = getToolById(id);
        if (tool == null) throw new IllegalArgumentException("Tool not found");

        tool.setStatus(Tool.Status.DEACTIVATED);
        tool.setUpdatedAt(LocalDateTime.now());
        toolRepository.saveAndFlush(tool);
    }

    // ==========================================================
    // 🔹 UPDATE TOOL
    // ==========================================================

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

        existingTool.setToolName(updatedTool.getToolName());
        existingTool.setDescription(updatedTool.getDescription());
        existingTool.setNote(updatedTool.getNote());
        existingTool.setUpdatedAt(LocalDateTime.now());

        if (updatedTool.getQuantity() != null)
            existingTool.setQuantity(updatedTool.getQuantity());

        if (updatedTool.getCategory() != null && updatedTool.getCategory().getCategoryId() != null) {
            Category realCategory = categoryRepository.findById(updatedTool.getCategory().getCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Category not found."));
            existingTool.setCategory(realCategory);
        }

        if (imagePath != null && !imagePath.isBlank())
            existingTool.setImage(imagePath);

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

        // 🔹 Cập nhật License (chỉ update nội dung, không clear list để tránh mất dữ liệu)
        if (licenseDays != null && licensePrices != null && licenseDays.size() == licensePrices.size()) {
            List<License> existingLicenses = licenseRepository.findByTool_ToolId(existingTool.getToolId());

            for (int i = 0; i < licenseDays.size(); i++) {
                License lic;
                if (i < existingLicenses.size()) {
                    lic = existingLicenses.get(i);
                } else {
                    lic = new License();
                    lic.setTool(existingTool);
                    existingLicenses.add(lic);
                }
                lic.setName("License " + licenseDays.get(i) + " days");
                lic.setDurationDays(licenseDays.get(i));
                lic.setPrice(licensePrices.get(i));
                licenseRepository.save(lic);
            }
        }

        toolRepository.save(existingTool);
    }

    // ==========================================================
    // 🔹 CATEGORY & LICENSE HANDLERS
    // ==========================================================

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

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
    public void createLicenseAccountsForLicenses(List<License> licenses, List<String> tokens) {
        if (licenses == null || licenses.isEmpty())
            throw new IllegalArgumentException("Tool must have at least one License to create accounts.");

        License primaryLicense = licenses.get(0);

        for (String token : tokens) {
            if (licenseAccountRepository.existsByToken(token)) {
                throw new IllegalArgumentException("Duplicate token detected: " + token);
            }

            LicenseAccount acc = new LicenseAccount();
            acc.setLicense(primaryLicense);
            acc.setToken(token);
            acc.setStatus(LicenseAccount.Status.ACTIVE);
            acc.setStartDate(LocalDateTime.now());
            acc.setEndDate(LocalDateTime.now().plusDays(primaryLicense.getDurationDays()));
            licenseAccountRepository.save(acc);
        }
    }

    // ==========================================================
    // 🔹 QUẢN LÝ TOOL NÂNG CAO
    // ==========================================================

    public void changeToolStatus(Long id, Tool.Status status) {
        Tool tool = getToolById(id);
        tool.setStatus(status);
        tool.setUpdatedAt(LocalDateTime.now());
        toolRepository.save(tool);
    }

    public boolean existsByToolName(String name) {
        return toolRepository.existsByToolName(name);
    }

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
            } catch (IllegalArgumentException ignored) {}
        }

        Tool.LoginMethod loginEnum = null;
        if (loginMethod != null && !loginMethod.isBlank()) {
            try {
                loginEnum = Tool.LoginMethod.valueOf(loginMethod.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }
        return toolRepository.searchToolsForSeller(
                sellerId, keyword, categoryId, statusEnum, loginEnum, minPrice, maxPrice, pageable
        );
    }

    // ==========================================================
    // 🔹 FIXED: updateQuantityAndLicenses (không còn clear list)
    // ==========================================================

    @Transactional
    public void updateQuantityAndLicenses(Long toolId, int newQuantity, List<License> newLicenses) {
        Tool tool = toolRepository.findById(toolId)
                .orElseThrow(() -> new IllegalArgumentException("Tool not found with id: " + toolId));

        List<License> existingLicenses = licenseRepository.findByTool_ToolId(toolId);

        for (int i = 0; i < newLicenses.size(); i++) {
            License src = newLicenses.get(i);
            License target;

            if (i < existingLicenses.size()) {
                target = existingLicenses.get(i);
            } else {
                target = new License();
                target.setTool(tool);
                existingLicenses.add(target);
            }

            target.setName(src.getName());
            target.setDurationDays(src.getDurationDays());
            target.setPrice(src.getPrice());
            licenseRepository.save(target);
        }

        if (existingLicenses.size() > newLicenses.size()) {
            for (int i = newLicenses.size(); i < existingLicenses.size(); i++) {
                licenseRepository.delete(existingLicenses.get(i));
            }
        }

        tool.setQuantity(newQuantity);
        tool.setUpdatedAt(LocalDateTime.now());
        toolRepository.save(tool);
    }
}
