package swp391.fa25.lms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp391.fa25.lms.model.Role;

@Repository
public interface RoleRepo extends JpaRepository<Role, Integer> {
}
