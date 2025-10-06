package swp391.fa25.lms.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import swp391.fa25.lms.model.Tool;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.repository.ToolRepo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ToolService {

    @Autowired
    private ToolRepo toolRepo;

    public Tool addTool(Tool tool, Account seller) {
        tool.setSeller(seller);
        tool.setStatus(Tool.Status.PENDING);
        tool.setCreatedAt(LocalDateTime.now());
        tool.setUpdatedAt(LocalDateTime.now());
        return toolRepo.save(tool);
    }

    public Tool updateTool(Long id, Tool newToolData, Account seller) {
        Tool tool = toolRepo.findById(id).orElseThrow(() -> new RuntimeException("Tool not found"));
        if (!tool.getSeller().equals(seller)) {
            throw new RuntimeException("You are not allowed to edit this tool");
        }
        tool.setToolName(newToolData.getToolName());
        tool.setDescription(newToolData.getDescription());
        tool.setImage(newToolData.getImage());
        tool.setCategory(newToolData.getCategory());
        tool.setUpdatedAt(LocalDateTime.now());
        return toolRepo.save(tool);
    }

    public void deleteTool(Long id, Account seller) {
        Tool tool = toolRepo.findById(id).orElseThrow(() -> new RuntimeException("Tool not found"));
        if (!tool.getSeller().equals(seller)) {
            throw new RuntimeException("You are not allowed to delete this tool");
        }
        toolRepo.delete(tool);
    }

    public List<Tool> getToolsBySeller(Account seller) {
        return toolRepo.findBySeller(seller);
    }
}
