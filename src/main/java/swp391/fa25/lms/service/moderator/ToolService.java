package swp391.fa25.lms.service.moderator;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import swp391.fa25.lms.model.Tool;
import swp391.fa25.lms.repository.CategoryRepository;
import swp391.fa25.lms.repository.ToolRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.criteria.Predicate;


@Service("moderatorToolService")
public class ToolService {
    @Autowired
    ToolRepository toolRepository;
    @Autowired
    CategoryRepository categoryRepository;

    public Tool findById(long id) {
        return toolRepository.findByToolId(id);
    }
    public void save(Tool tool) {
        toolRepository.save(tool);
    }
    public List<Tool> getPendingTools() {
        return toolRepository.findByStatus(Tool.Status.PENDING);
    }
    public List<Tool> filterPendingTools(
            Long sellerId,
            Long categoryId,
            LocalDateTime uploadFrom,
            LocalDateTime uploadTo
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

            predicates.add(cb.equal(root.get("status"), Tool.Status.PENDING));

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return toolRepository.findAll(spec);
    }
    public void approveTool(Tool tool) {
        tool.setStatus(Tool.Status.APPROVED);
        tool.setUpdatedAt(LocalDateTime.now());
        tool.setNote(null);
        toolRepository.save(tool);
    }
    public void rejectTool(Tool tool, String reason) {
        tool.setStatus(Tool.Status.REJECTED);
        tool.setNote(reason);
        tool.setUpdatedAt(LocalDateTime.now());
        toolRepository.save(tool);
    }
    public List<Tool> getNonPendingTools() {
        return toolRepository.findByStatusNot(Tool.Status.PENDING);
    }

    public List<Tool> filterNonPendingTools(
            Long sellerId,
            Long categoryId,
            LocalDateTime uploadFrom,
            LocalDateTime uploadTo,
            LocalDateTime approvedFrom,
            LocalDateTime approvedTo,
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


            if (status != null && !status.isEmpty()) {
                predicates.add(cb.equal(root.get("status"), Tool.Status.valueOf(status)));
            }

            // Loại bỏ các tool đang chờ duyệt (PENDING)
            predicates.add(cb.notEqual(root.get("status"), Tool.Status.PENDING));

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return toolRepository.findAll(spec);
    }

}
