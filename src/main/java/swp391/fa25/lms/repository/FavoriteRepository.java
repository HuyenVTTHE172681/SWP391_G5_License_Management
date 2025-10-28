package swp391.fa25.lms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
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
//    Optional<Favorite> findByAccountAndTool(Long accountId);
}
