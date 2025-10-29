package swp391.fa25.lms.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "ToolReport")
public class ToolReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tool_report_id")
    private Long toolReportId;

    // Người report
    @ManyToOne(optional = false)
    @JoinColumn(name = "reporter_id", nullable = false)
    private Account reporter;

    // Tool bị report
    @ManyToOne(optional = false)
    @JoinColumn(name = "tool_id", nullable = false)
    private Tool tool;

    // Lý do report
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Reason reason;

    @Column(columnDefinition = "NVARCHAR(500)")
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDING;

    @Column(nullable = false)
    private LocalDateTime reportedAt = LocalDateTime.now();

    public enum Status {
        PENDING, APPROVED, REJECTED
    }

    public enum Reason {
        SPAM("Tool chứa nội dung spam hoặc quảng cáo"),
        MALICIOUS_CONTENT("Nội dung độc hại, chứa virus, hoặc liên kết nguy hiểm"),
        COPYRIGHT_VIOLATION("Vi phạm bản quyền hoặc sao chép từ tool khác"),
        INAPPROPRIATE_CONTENT("Nội dung không phù hợp hoặc trái đạo đức"),
        MISLEADING_INFO("Thông tin mô tả sai lệch về chức năng"),
        IRRELEVANT_CATEGORY("Tool bị đăng sai danh mục"),
        OTHER("Khác (người dùng nhập mô tả)");

        private final String description;
        Reason(String description) {
            this.description = description;
        }
        public String getDescription() {
            return description;
        }
    }

    public ToolReport() {}

    public ToolReport(Account reporter, Tool tool, Reason reason, String comment) {
        this.reporter = reporter;
        this.tool = tool;
        this.reason = reason;
        this.comment = comment;
        this.reportedAt = LocalDateTime.now();
        this.status = Status.PENDING;
    }

    // --- Getters & Setters ---
    public Long getToolReportId() {
        return toolReportId;
    }

    public void setToolReportId(Long toolReportId) {
        this.toolReportId = toolReportId;
    }

    public Account getReporter() {
        return reporter;
    }

    public void setReporter(Account reporter) {
        this.reporter = reporter;
    }

    public Tool getTool() {
        return tool;
    }

    public void setTool(Tool tool) {
        this.tool = tool;
    }

    public Reason getReason() {
        return reason;
    }

    public void setReason(Reason reason) {
        this.reason = reason;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public LocalDateTime getReportedAt() {
        return reportedAt;
    }

    public void setReportedAt(LocalDateTime reportedAt) {
        this.reportedAt = reportedAt;
    }
}
