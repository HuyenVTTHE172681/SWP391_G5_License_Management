package swp391.fa25.lms.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDING;

    @Column(nullable = false)
    private LocalDate reportedAt = LocalDate.now();

    public enum Status {
        PENDING, APPROVED, REJECTED
    }

    public enum Reason {
        SPAM,
        MALICIOUS_CONTENT,
        COPYRIGHT_VIOLATION,
        INAPPROPRIATE_CONTENT,
        MISLEADING_INFO,
        IRRELEVANT_CATEGORY,
        OTHER;
    }
        private String description;

    public ToolReport() {
    }

    public ToolReport(Account reporter, Tool tool, Reason reason, String description) {
        this.reporter = reporter;
        this.tool = tool;
        this.reason = reason;
        this.description = description;
        this.reportedAt = LocalDate.now();
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public LocalDate getReportedAt() {
        return reportedAt;
    }

    public void setReportedAt(LocalDate reportedAt) {
        this.reportedAt = reportedAt;
    }
}
