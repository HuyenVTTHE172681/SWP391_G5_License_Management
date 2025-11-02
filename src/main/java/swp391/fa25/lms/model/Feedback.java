package swp391.fa25.lms.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Feedback")
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feedback_id")
    private Long feedbackId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    @JsonBackReference(value = "feedback-account")
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tool_id", nullable = false)
    @JsonBackReference(value = "feedback-tool")
    private Tool tool;

    @Column(nullable = false)
    private Integer rating;

    @Column(columnDefinition = "NVARCHAR(100)")
    private String comment;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public Feedback() {
    }

    public Feedback(Long feedbackId, Account account, Tool tool, Integer rating, String comment, LocalDateTime createdAt) {
        this.feedbackId = feedbackId;
        this.account = account;
        this.tool = tool;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = createdAt;
    }

    public Long getFeedbackId() {
        return feedbackId;
    }

    public void setFeedbackId(Long feedbackId) {
        this.feedbackId = feedbackId;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public Tool getTool() {
        return tool;
    }

    public void setTool(Tool tool) {
        this.tool = tool;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}