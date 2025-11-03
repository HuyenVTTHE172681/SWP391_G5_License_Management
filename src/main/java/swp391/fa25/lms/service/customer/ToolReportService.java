package swp391.fa25.lms.service.customer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.Tool;
import swp391.fa25.lms.model.ToolReport;
import swp391.fa25.lms.repository.ToolReportRepository;
import swp391.fa25.lms.repository.ToolRepository;

import java.time.LocalDate;

@Service
public class ToolReportService {
    @Autowired
    private ToolReportRepository toolReportRepository;
    @Autowired
    private ToolRepository toolRepository;

    public String reportTool(Account reporter, Long toolId, ToolReport.Reason reason, String description) {
        Tool tool = toolRepository.findById(toolId)
                .orElseThrow(() -> new RuntimeException("Tool không tồn tại"));

        if (toolReportRepository.existsByReporterAndTool(reporter, tool)) {
            return "Bạn đã báo cáo tool này trước đó!";
        }

        ToolReport report = new ToolReport();
        report.setReporter(reporter);
        report.setTool(tool);
        report.setReason(reason);
        report.setDescription(description);
        report.setStatus(ToolReport.Status.PENDING);
        report.setReportedAt(LocalDate.now());
        tool.setStatus(Tool.Status.SUSPECT);

        toolReportRepository.save(report);
        return "Báo cáo của bạn đã được gửi thành công. Hệ thống sẽ xem xét!";
    }
}
