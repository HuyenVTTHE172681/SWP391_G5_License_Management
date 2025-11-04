package swp391.fa25.lms.service.manager;

import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import swp391.fa25.lms.model.Tool;
import swp391.fa25.lms.repository.ToolRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service("manageToolService")
public class ToolService {
    @Autowired
    private ToolRepository toolRepository;

    public Tool findById(long id) {
        return toolRepository.findByToolId(id);
    }

    public List<Tool> filterApprovedTools(
            Long sellerId,
            Long categoryId,
            LocalDateTime uploadFrom,
            LocalDateTime uploadTo,
            LocalDateTime approvedFrom,
            LocalDateTime approvedTo
    ) {
        Specification<Tool> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (sellerId != null) {
                predicates.add(cb.equal(root.get("seller").get("accountId"), sellerId));
            }

            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("categoryId"), categoryId));
            }

            if (uploadFrom != null && uploadTo != null) {
                predicates.add(cb.between(root.get("createdAt"), uploadFrom, uploadTo));
            } else if (uploadFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), uploadFrom));
            } else if (uploadTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), uploadTo));
            }

            if (approvedFrom != null && approvedTo != null) {
                predicates.add(cb.between(root.get("updatedAt"), approvedFrom, approvedTo));
            } else if (approvedFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("updatedAt"), approvedFrom));
            } else if (approvedTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("updatedAt"), approvedTo));
            }


            predicates.add(cb.equal(root.get("status"), Tool.Status.APPROVED));

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return toolRepository.findAll(spec);
    }

    public void save(Tool tool) {
        toolRepository.save(tool);
    }

    public List<Tool> filterNonPendingTools(
            Long sellerId,
            Long categoryId,
            LocalDateTime uploadFrom,
            LocalDateTime uploadTo,
            LocalDateTime approvedFrom,
            LocalDateTime approvedTo,
            String reviewedBy,
            String status
    ) {
        Specification<Tool> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (sellerId != null) {
                predicates.add(cb.equal(root.get("seller").get("accountId"), sellerId));
            }

            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("categoryId"), categoryId));
            }

            if (uploadFrom != null && uploadTo != null) {
                predicates.add(cb.between(root.get("createdAt"), uploadFrom, uploadTo));
            } else if (uploadFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), uploadFrom));
            } else if (uploadTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), uploadTo));
            }

            if (approvedFrom != null && approvedTo != null) {
                predicates.add(cb.between(root.get("updatedAt"), approvedFrom, approvedTo));
            } else if (approvedFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("updatedAt"), approvedFrom));
            } else if (approvedTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("updatedAt"), approvedTo));
            }
            if (reviewedBy != null && !reviewedBy.isBlank()) {
                if (reviewedBy.equalsIgnoreCase("NULL")) {
                    predicates.add(cb.isNull(root.get("reviewedBy")));
                } else {
                    predicates.add(cb.equal(root.get("reviewedBy"), reviewedBy));
                }
            }

            if (status != null && !status.isEmpty()) {
                predicates.add(cb.equal(root.get("status"), Tool.Status.valueOf(status)));
            }

            predicates.add(cb.notEqual(root.get("status"), Tool.Status.PENDING));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return toolRepository.findAll(spec);
    }
    /** 📈 Biểu đồ tăng trưởng tool theo tháng */
    public Map<Integer, Long> getToolGrowthByMonth() {
        return toolRepository.getToolGrowthByMonth().stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).intValue(),
                        row -> ((Number) row[1]).longValue()
                ));
    }

    /** 🧩 Tỷ lệ tool theo status */
    public Map<String, Long> getToolStatusDistribution() {
        return toolRepository.getToolCountByStatus().stream()
                .collect(Collectors.toMap(
                        row -> row[0].toString(),
                        row -> ((Number) row[1]).longValue()
                ));
    }
}
