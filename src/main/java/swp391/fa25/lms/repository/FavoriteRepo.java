package swp391.fa25.lms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp391.fa25.lms.model.Favorite;

@Repository
public interface FavoriteRepo extends JpaRepository<Favorite, Long> {
}
