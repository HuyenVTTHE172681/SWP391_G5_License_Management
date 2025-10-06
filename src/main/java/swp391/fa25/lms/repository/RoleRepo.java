package swp391.fa25.lms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp391.fa25.lms.model.Role;

import java.util.Optional;

@Repository
public interface RoleRepo extends JpaRepository<Role, Integer> {
    boolean existsByRoleName(Role.RoleName roleName);
    Optional<Role> findByRoleName(Role.RoleName roleName);
}
