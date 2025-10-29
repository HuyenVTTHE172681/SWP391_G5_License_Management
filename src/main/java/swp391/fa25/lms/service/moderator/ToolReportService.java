package swp391.fa25.lms.service.moderator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import swp391.fa25.lms.model.ToolReport;
import swp391.fa25.lms.repository.ToolReportRepository;

import java.util.List;

@Service("moderatorToolReportService")
public class ToolReportService {
    @Autowired
    private ToolReportRepository toolReportRepository;
    public List<ToolReport> findAllToolReport() {
        return toolReportRepository.findAll();
    }
}
