package swp391.fa25.lms.service.moderator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import swp391.fa25.lms.model.Tool;
import swp391.fa25.lms.model.ToolReport;
import swp391.fa25.lms.repository.ToolReportRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service("moderatorToolReportService")
public class ToolReportService {
    @Autowired
    private ToolReportRepository toolReportRepository;

    public List<ToolReport> findAllToolReport() {
        return toolReportRepository.findAll();
    }

    public List<ToolReport> filterReports(ToolReport.Status status,
                                          Long toolId,
                                          Long reporterId,
                                          LocalDate fromDate,
                                          LocalDate toDate) {

        Specification<ToolReport> spec = Specification.allOf();

        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }

        if (toolId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("tool").get("toolId"), toolId));
        }

        if (reporterId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("reporter").get("accountId"), reporterId));
        }

        if (fromDate != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("reportedAt"), fromDate));
        }

        if (toDate != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("reportedAt"), toDate));
        } else if (status == null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), ToolReport.Status.PENDING));
        }

        return toolReportRepository.findAll(spec);
    }

    public void updateStatus(Long id, ToolReport.Status status) {
        ToolReport report = toolReportRepository.findById(id).orElse(null);
        if (report != null && report.getTool() != null) {
            report.setStatus(status);
            toolReportRepository.save(report);

            if (status.equals(ToolReport.Status.APPROVED)) {
                List<ToolReport> relatedReports = toolReportRepository.findToolReportByTool(report.getTool());

                for (ToolReport r : relatedReports) {
                        r.setStatus(ToolReport.Status.APPROVED);
                }
                toolReportRepository.saveAll(relatedReports);

                report.getTool().setStatus(Tool.Status.VIOLATED);
            } else if (status.equals(ToolReport.Status.REJECTED)) {
                report.getTool().setStatus(Tool.Status.SUSPECT);
            }
            if (!toolReportRepository.existsByToolAndStatusNot(report.getTool(), ToolReport.Status.APPROVED)) {
                report.getTool().setStatus(Tool.Status.PUBLISHED);
            }
        }

    }
}
