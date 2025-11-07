package swp391.fa25.lms.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "FeedbackReport")
public class FeedbackReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long feedbackReportId;

    @ManyToOne
    @JoinColumn(name = "feedback_id")
    private Feedback feedback;

    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account reportedBy;

    @Enumerated(EnumType.STRING)
    private Status status;
    public enum Status {PENDING, REJECTED, APPROVED}

    @Enumerated(EnumType.STRING)
    private Reason reason;

    public enum Reason {SPAM, OFFENSIVE_LANGUAGE, FALSE_INFORMATION, IRRELEVANT, HARASSMENT, PRIVACY_VIOLATION, OTHER}

    private String description;
    @Column(nullable = false)
    private LocalDateTime reportedAt = LocalDateTime.now();

    public FeedbackReport() {

    }
    public FeedbackReport(Long feedbackReportId, Feedback feedback, Account reportedBy, Status status, Reason reason, String description, LocalDateTime reportedAt) {
        this.feedbackReportId = feedbackReportId;
        this.feedback = feedback;
        this.reportedBy = reportedBy;
        this.status = status;
        this.reason = reason;
        this.description = description;
        this.reportedAt = reportedAt;
    }

    public Long getFeedbackReportId() {
        return feedbackReportId;
    }

    public void setFeedbackReportId(Long feedbackReportId) {
        this.feedbackReportId = feedbackReportId;
    }

    public Feedback getFeedback() {
        return feedback;
    }

    public void setFeedback(Feedback feedback) {
        this.feedback = feedback;
    }

    public Account getReportedBy() {
        return reportedBy;
    }

    public void setReportedBy(Account reportedBy) {
        this.reportedBy = reportedBy;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
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

    public LocalDateTime getReportedAt() {
        return reportedAt;
    }

    public void setReportedAt(LocalDateTime reportedAt) {
        this.reportedAt = reportedAt;
    }
}
