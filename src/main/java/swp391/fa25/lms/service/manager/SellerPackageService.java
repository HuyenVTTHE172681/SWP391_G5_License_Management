package swp391.fa25.lms.service.manager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import swp391.fa25.lms.model.SellerPackage;
import swp391.fa25.lms.repository.SellerPackageRepository;

import java.util.ArrayList;
import java.util.List;

@Service("managerSellerPackageService")
public class SellerPackageService {
    @Autowired
    private SellerPackageRepository repository;

    public List<SellerPackage> getAll() {
        return repository.findAll();
    }

    public List<SellerPackage> filter(String packageName,
                                      Double minPrice,
                                      Double maxPrice,
                                      Integer minDuration,
                                      Integer maxDuration,
                                      SellerPackage.Status status) {

        Specification<SellerPackage> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (packageName != null && !packageName.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("packageName")), "%" + packageName.toLowerCase() + "%"));
            }

            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            }

            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            }

            if (minDuration != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("durationInMonths"), minDuration));
            }

            if (maxDuration != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("durationInMonths"), maxDuration));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        return repository.findAll(spec);
    }

    public SellerPackage findById(int id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid package ID: " + id));
    }

    public SellerPackage save(SellerPackage sp) {
        return repository.save(sp);
    }
    public boolean isExist(String packageName) {
        return repository.existsByPackageName(packageName);
    }
}
