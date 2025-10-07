package swp391.fa25.lms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp391.fa25.lms.model.Account;

@Repository
public interface AccountRepo extends JpaRepository<Account, Long> {
    Account findByEmail(String email);

}

