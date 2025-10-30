package swp391.fa25.lms.service.seller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.LicenseAccount;
import swp391.fa25.lms.model.Tool;
import swp391.fa25.lms.repository.LicenseAccountRepository;
import swp391.fa25.lms.repository.ToolFileRepository;
import swp391.fa25.lms.repository.ToolRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service("seller")
public class ToolService {

    @Autowired
    private ToolFileRepository toolFileRepository;

    @Autowired
    private ToolRepository toolRepo;

    @Autowired
    private LicenseAccountRepository licenseAccountRepo;

    @Transactional
    public Tool addTool(Tool tool, Account seller) {
        tool.setSeller(seller);
        tool.setStatus(Tool.Status.PENDING);
        tool.setCreatedAt(LocalDateTime.now());
        tool.setUpdatedAt(LocalDateTime.now());
        return toolRepo.save(tool);
    }


    public Tool save(Tool tool) {
        tool.setUpdatedAt(LocalDateTime.now());
        return toolRepo.save(tool);
    }

    /**
     *  Update tool info + quantity validation
     */
    @Transactional
    public Tool updateTool(Long id, Tool newToolData, Account seller) {
        Tool tool = toolRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Tool not found"));

        // 1 Kiểm tra xem tool này có thuộc seller đang đăng nhập không
        if (!tool.getSeller().getAccountId().equals(seller.getAccountId())) {
            throw new RuntimeException("You are not allowed to edit this tool");
        }

        // 2 Đếm số lượng token key đã cấp (đã dùng)
        long usedTokenCount = licenseAccountRepo.countByToolToolIdAndLoginMethod(
                id, LicenseAccount.LoginMethod.TOKEN
        );

        // 3 Kiểm tra quantity hợp lệ
        Integer newQuantity = newToolData.getQuantity();
        if (newQuantity != null && newQuantity < usedTokenCount) {
            throw new RuntimeException("Quantity cannot be less than the number of used token keys (" + usedTokenCount + ")");
        }

        // 4 Cập nhật các field
        tool.setToolName(newToolData.getToolName());
        tool.setDescription(newToolData.getDescription());
        tool.setImage(newToolData.getImage());
        tool.setCategory(newToolData.getCategory());
        tool.setUpdatedAt(LocalDateTime.now());

        if (newQuantity != null) {
            tool.setQuantity(newQuantity);
        }

        return toolRepo.save(tool);
    }
    @Transactional
    public void toggleToolStatus(Long id, Account seller) {
        Tool tool = toolRepo.findByToolIdAndSeller(id, seller)
                .orElseThrow(() -> new RuntimeException("Tool not found or unauthorized"));

        if (tool.getStatus() == Tool.Status.PUBLISHED) {
            tool.setStatus(Tool.Status.DEACTIVATED);
        } else {
            tool.setStatus(Tool.Status.PUBLISHED);
        }

        tool.setUpdatedAt(LocalDateTime.now());
        toolRepo.save(tool);
    }

    public List<Tool> getToolsBySeller(Account seller) {
        return toolRepo.findBySeller(seller);
    }

    public Tool getToolById(Long id) {
        return toolRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Tool not found"));
    }

}
