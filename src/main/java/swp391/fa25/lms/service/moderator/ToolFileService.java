package swp391.fa25.lms.service.moderator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import swp391.fa25.lms.model.Tool;
import swp391.fa25.lms.model.ToolFile;
import swp391.fa25.lms.repository.ToolFileRepository;

import java.util.List;

@Service("moderatorToolFileService")
public class ToolFileService {
    @Autowired
    ToolFileRepository toolFileRepository;
    public List<ToolFile> findByTool(Tool tool) {
        return toolFileRepository.findByTool(tool);
    }

}
