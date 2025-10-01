package swp391.fa25.lms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import swp391.fa25.lms.model.Account;

public interface AccountRepo extends JpaRepository<Account, Integer> {
}
