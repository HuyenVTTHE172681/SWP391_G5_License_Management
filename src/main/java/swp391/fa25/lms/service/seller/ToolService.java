package swp391.fa25.lms.service.seller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import swp391.fa25.lms.model.Tool;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.repository.ToolFileRepository;
import swp391.fa25.lms.repository.ToolRepository;

import java.time.LocalDateTime;
import java.util.List;


@Service("seller")
public class ToolService {
    @Autowired
    private ToolFileRepository toolFileRepository;;
    @Autowired
    private ToolRepository toolRepo;

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
    public Tool updateTool(Long id, Tool newToolData, Account seller) {
        Tool tool = toolRepo.findById(id).orElseThrow(() -> new RuntimeException("Tool not found"));

        tool.setToolName(newToolData.getToolName());
        tool.setDescription(newToolData.getDescription());
        tool.setImage(newToolData.getImage());
        tool.setCategory(newToolData.getCategory());
        tool.setUpdatedAt(LocalDateTime.now());
        return toolRepo.save(tool);
    }

    public void deleteTool(Long id, Account seller) {
        Tool tool = toolRepo.findById(id).orElseThrow(() -> new RuntimeException("Tool not found"));
        toolFileRepository.deleteAllByToolToolId(id);

        toolRepo.delete(tool);
    }

    public List<Tool> getToolsBySeller(Account seller) {
        return toolRepo.findBySeller(seller);
    }

    // ✅ Lấy tool theo ID (thêm để dùng cho edit)
    public Tool getToolById(Long id) {
        return toolRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Tool not found"));
    }
}
