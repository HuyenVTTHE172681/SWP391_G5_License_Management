package swp391.fa25.lms.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "feedback_reply")
public class FeedbackReply {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reply_id")
    private Long replyId;

    @OneToOne
    @JoinColumn(name = "feedback_id", nullable = false, unique = true)
    private Feedback feedback; // mỗi feedback có tối đa 1 reply của seller

    @ManyToOne
    @JoinColumn(name = "seller_id", nullable = false)
    private Account seller; // seller của tool

    @Column(name = "content", columnDefinition = "NVARCHAR(500)", nullable = false)
    private String content;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // getters/setters...

    public Long getReplyId() {
        return replyId;
    }

    public void setReplyId(Long replyId) {
        this.replyId = replyId;
    }

    public Feedback getFeedback() {
        return feedback;
    }

    public void setFeedback(Feedback feedback) {
        this.feedback = feedback;
    }

    public Account getSeller() {
        return seller;
    }

    public void setSeller(Account seller) {
        this.seller = seller;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}