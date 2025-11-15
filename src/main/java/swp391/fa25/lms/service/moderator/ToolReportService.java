package swp391.fa25.lms.service.moderator;

import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import swp391.fa25.lms.model.Tool;
import swp391.fa25.lms.model.ToolReport;
import swp391.fa25.lms.repository.ToolReportRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service("moderatorToolReportService")
public class ToolReportService {
    @Autowired
    private ToolReportRepository toolReportRepository;

    public List<ToolReport> findAllToolReport() {
        return toolReportRepository.findAll();
    }

    public Page<ToolReport> filterReports(
            ToolReport.Status status,
            Long toolId,
            Long reporterId,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable
    ) {
        Specification<ToolReport> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Status
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            // Tool ID
            if (toolId != null) {
                predicates.add(cb.equal(root.get("tool").get("toolId"), toolId));
            }

            // Reporter ID
            if (reporterId != null) {
                predicates.add(cb.equal(root.get("reporter").get("accountId"), reporterId));
            }

            // Date range (fromDate / toDate là LocalDate, reportedAt cũng nên là LocalDate)
            if (fromDate != null && toDate != null) {
                predicates.add(cb.between(root.get("reportedAt"), fromDate, toDate));
            } else if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("reportedAt"), fromDate));
            } else if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("reportedAt"), toDate));
            }

            // Default: nếu không chọn status và cũng không chọn toDate thì chỉ lấy PENDING
            if (status == null && toDate == null) {
                predicates.add(cb.equal(root.get("status"), ToolReport.Status.PENDING));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return toolReportRepository.findAll(spec, pageable);
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
    public List<ToolReport> findByStatus(ToolReport.Status status) {
        return toolReportRepository.findByStatus(status);
    }
}
