package swp391.fa25.lms.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Favorite")
public class Favorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long favoriteId;

    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"favorites", "tools", "wallet", "orders"})
    private Account account;

    @ManyToOne
    @JoinColumn(name = "tool_id")
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"seller", "licenses", "files", "orders"})
    private Tool tool;

    public Favorite() {
    }

    public Favorite(Long favoriteId, Account account, Tool tool) {
        this.favoriteId = favoriteId;
        this.account = account;
        this.tool = tool;
    }

    public Long getFavoriteId() {
        return favoriteId;
    }

    public void setFavoriteId(Long favoriteId) {
        this.favoriteId = favoriteId;
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
}
