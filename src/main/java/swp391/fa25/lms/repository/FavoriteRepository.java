package swp391.fa25.lms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.Favorite;
import swp391.fa25.lms.model.Tool;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    Optional<Favorite> findByAccountAndTool(Account account, Tool tool);
    List<Favorite> findByAccount(Account account);
    long countByAccount(Account account);

    // Query by account ID (dùng @Query để lấy list Favorite)
    @Query("SELECT f FROM Favorite f WHERE f.account.accountId = :accountId")
    List<Favorite> findByAccountId(@Param("accountId") Long accountId);

    // Count by account ID (an toàn hơn, tránh detached entity)
    @Query("SELECT COUNT(f) FROM Favorite f WHERE f.account.accountId = :accountId")
    long countByAccountId(@Param("accountId") Long accountId);
}
