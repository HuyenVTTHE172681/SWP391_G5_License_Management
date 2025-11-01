package swp391.fa25.lms.service.seller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.fa25.lms.model.*;
import swp391.fa25.lms.repository.*;

import java.time.LocalDateTime;
import java.util.List;

@Service("sellerToolService")
@Transactional
public class ToolService {

    @Autowired
    private ToolRepository toolRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private LicenseToolRepository licenseRepository;
    @Autowired
    private LicenseAccountRepository licenseAccountRepository;

    /**
     * ✅ Tạo mới Tool (chỉ lưu DB, không xử lý file hoặc token)
     * Sau khi tạo → luôn ở trạng thái PENDING
     */
    public Tool createTool(Tool tool, Category category) {
        tool.setCategory(category);
        tool.setStatus(Tool.Status.PENDING); // luôn ở trạng thái chờ duyệt
        tool.setCreatedAt(LocalDateTime.now());
        tool.setUpdatedAt(LocalDateTime.now());
        return toolRepository.save(tool);
    }

    /**
     * ✅ Cập nhật Tool hiện có
     */
    public Tool updateTool(Long id, Tool updatedTool) {
        Tool existing = toolRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tool not found with ID: " + id));

        existing.setToolName(updatedTool.getToolName());
        existing.setDescription(updatedTool.getDescription());
        existing.setCategory(updatedTool.getCategory());
        existing.setLoginMethod(updatedTool.getLoginMethod());
        existing.setNote(updatedTool.getNote());
        existing.setQuantity(updatedTool.getQuantity());
        existing.setUpdatedAt(LocalDateTime.now());

        // Khi update tool, nếu có thay đổi lớn, có thể set lại PENDING để admin duyệt lại
        existing.setStatus(Tool.Status.PENDING);

        return toolRepository.save(existing);
    }

    /**
     * ✅ “Xóa” tool — thực tế là chuyển trạng thái sang DEACTIVE
     */
    public void deactivateTool(Long id) {
        Tool tool = toolRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tool not found with ID: " + id));
        tool.setStatus(Tool.Status.DEACTIVE);
        tool.setUpdatedAt(LocalDateTime.now());
        toolRepository.save(tool);
    }

    /**
     * ✅ Lấy tool theo ID
     */
    public Tool getToolById(Long id) {
        return toolRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tool not found"));
    }

    /**
     * ✅ Lấy toàn bộ tool của 1 seller (trừ tool DEACTIVE)
     */
    public List<Tool> getToolsBySeller(Account seller) {
        return toolRepository.findBySellerAndStatusNot(seller, Tool.Status.DEACTIVE);
    }

    /**
     * ✅ Lấy tất cả category (dùng cho dropdown)
     */
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    /**
     * ✅ Tạo License cho Tool
     */
    public void createLicensesForTool(Tool tool, List<License> licenses) {
        for (License license : licenses) {
            license.setTool(tool);
            license.setCreatedAt(LocalDateTime.now());
            licenseRepository.save(license);
        }
    }

    /**
     * ✅ Tạo LicenseAccount cho Tool (nếu loginMethod = TOKEN)
     */
    public void createLicenseAccountsForTool(Tool tool, List<String> tokens) {
        for (String token : tokens) {
            LicenseAccount acc = new LicenseAccount();
            acc.setTool(tool);
            acc.setLoginMethod(LicenseAccount.LoginMethod.TOKEN);
            acc.setToken(token);
            acc.setStatus(LicenseAccount.Status.ACTIVE);
            acc.setCreatedAt(LocalDateTime.now());
            licenseAccountRepository.save(acc);
        }
    }

    /**
     * ✅ Tìm Category theo ID (dùng để validate)
     */
    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));
    }

    /**
     * ✅ Đổi trạng thái Tool (VD: Admin duyệt)
     */
    public void changeToolStatus(Long id, Tool.Status status) {
        Tool tool = getToolById(id);
        tool.setStatus(status);
        tool.setUpdatedAt(LocalDateTime.now());
        toolRepository.save(tool);
    }

    /**
     * ✅ Kiểm tra trùng tên Tool
     */
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
        Tool.LoginMethod loginEnum = null;
        if (loginMethod != null && !loginMethod.isBlank()) {
            try {
                loginEnum = Tool.LoginMethod.valueOf(loginMethod);
            } catch (IllegalArgumentException ex) {
                // Nếu không khớp enum (vd: giá trị lạ) thì để null
                loginEnum = null;
            }
        }
        return toolRepository.searchToolsForSeller(
                sellerId, keyword, categoryId, status, loginEnum, minPrice, maxPrice, pageable
        );

    }
}
