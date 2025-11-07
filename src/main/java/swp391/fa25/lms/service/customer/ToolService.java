package swp391.fa25.lms.service.customer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.Feedback;
import swp391.fa25.lms.model.Tool;
import swp391.fa25.lms.repository.FeedbackRepository;
import swp391.fa25.lms.repository.ToolRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class ToolService {
    @Autowired private ToolRepository toolRepo;
    @Autowired private FeedbackRepository feedbackRepo;
    @Autowired private FavoriteService favoriteService;

    // ================== SEARCH + FILTER (giữ nguyên, chỉ đổi cách tính avg/count) ==================
    public Page<Tool> searchAndFilterTools(String keyword, Long categoryId, String dateFilter,
                                           String priceFilter, Integer ratingFilter,
                                           Account account, int page, int size) {

        List<Tool> tools = toolRepo.findAllPublishedAndSellerActive();

//        List<Tool> tools = toolRepo.findAll();
        // Search keyword (tool name hoặc seller name)
        if (keyword != null && !keyword.isEmpty()) {
            String kwLower = keyword.toLowerCase();
            tools = tools.stream()
                    .filter(t -> t.getToolName().toLowerCase().contains(kwLower)
                            || t.getSeller().getFullName().toLowerCase().contains(kwLower))
                    .toList();
        }

        // Filter theo category
        if (categoryId != null && categoryId > 0) {
            tools = tools.stream()
                    .filter(t -> t.getCategory() != null && t.getCategory().getCategoryId().equals(categoryId))
                    .toList();
        }

        // Filter theo ngày đăng
        if (dateFilter != null && !dateFilter.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            switch (dateFilter) {
                case "1" -> tools = tools.stream().sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt())).toList(); // mới nhất
                case "2" -> tools = tools.stream().filter(t -> t.getCreatedAt().isAfter(now.minusDays(30))).toList();
                case "3" -> tools = tools.stream().filter(t -> t.getCreatedAt().isAfter(now.minusMonths(3))).toList();
            }
        }

        // Tính min/max price + avg rating + total reviews (CHỈ tính feedback PUBLISHED hoặc NULL cho tương thích cũ)
        tools.forEach(tool -> {
            // Giá
            if (tool.getLicenses() != null && !tool.getLicenses().isEmpty()) {
                BigDecimal min = tool.getLicenses().stream()
                        .map(l -> BigDecimal.valueOf(l.getPrice()))
                        .min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
                BigDecimal max = tool.getLicenses().stream()
                        .map(l -> BigDecimal.valueOf(l.getPrice()))
                        .max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
                tool.setMinPrice(min);
                tool.setMaxPrice(max);
            } else {
                tool.setMinPrice(BigDecimal.ZERO);
                tool.setMaxPrice(BigDecimal.ZERO);
            }

            // Rating & total CHỈ tính PUBLISHED (hoặc status NULL)
            Double avg = feedbackRepo.avgRatingByToolAndStatusOrNull(tool, Feedback.Status.PUBLISHED);
            Long total = feedbackRepo.countByToolAndStatusOrNull(tool, Feedback.Status.PUBLISHED);
            tool.setAverageRating(avg != null ? avg : 0.0);
            tool.setTotalReviews(total != null ? total : 0L);
        });

        // Filter theo price
        if (priceFilter != null && !priceFilter.equals("all")) {
            tools = tools.stream().filter(t -> {
                BigDecimal min = t.getMinPrice() != null ? t.getMinPrice() : BigDecimal.ZERO;
                return switch (priceFilter) {
                    case "under100k" -> min.compareTo(BigDecimal.valueOf(100_000)) < 0;
                    case "100k-500k" -> min.compareTo(BigDecimal.valueOf(100_000)) >= 0
                            && min.compareTo(BigDecimal.valueOf(500_000)) <= 0;
                    case "500k-1m" -> min.compareTo(BigDecimal.valueOf(500_000)) > 0
                            && min.compareTo(BigDecimal.valueOf(1_000_000)) <= 0;
                    case "above1m" -> min.compareTo(BigDecimal.valueOf(1_000_000)) > 0;
                    default -> true;
                };
            }).toList();
        }

        // Filter theo rating
        if (ratingFilter != null && ratingFilter > 0) {
            tools = tools.stream().filter(t -> t.getAverageRating() >= ratingFilter).toList();
        }

        // isFavorite
        if (account != null) {
            Set<Long> favIds = favoriteService.getFavoriteToolIds(account);
            tools.forEach(t -> t.setIsFavorite(favIds.contains(t.getToolId())));
        } else {
            tools.forEach(t -> t.setIsFavorite(false));
        }

        // Pagination
        int start = page * size;
        int end = Math.min(start + size, tools.size());
        List<Tool> pagedList = tools.subList(Math.min(start, tools.size()), end);

        return new PageImpl<>(pagedList, PageRequest.of(page, size), tools.size());
    }

    // ================== TOOL ==================
    /** Lấy tool theo id, status = PUBLISHED */
    public Optional<Tool> findPublishedToolById(Long id) {
        return toolRepo.findByToolIdAndStatus(id, Tool.Status.PUBLISHED);
    }

    /** Lấy tool theo id (không lọc trạng thái) */
    public Optional<Tool> findById(Long id) {
        return toolRepo.findById(id);
    }

    public Tool getToolById(Long toolId) {
        return toolRepo.findById(toolId).orElse(null);
    }

    // ================== FEEDBACK (thêm overload có Status) ==================
    /** Mặc định: chỉ lấy feedback PUBLISHED (tương thích cũ) */
    public Page<Feedback> getFeedbackPageForTool(Tool tool, int page, int size) {
        return getFeedbackPageForTool(tool, page, size, Feedback.Status.PUBLISHED);
    }

    /** Lấy feedback theo trạng thái (coi NULL như PUBLISHED để tương thích dữ liệu cũ) */
    public Page<Feedback> getFeedbackPageForTool(Tool tool, int page, int size, Feedback.Status status) {
        Pageable pageable = PageRequest.of(page, size /* KHÔNG set Sort ở đây */);
        return feedbackRepo.findByToolAndStatusOrNull(tool, status, pageable);
    }

    /** Mặc định: avg chỉ tính PUBLISHED */
    public double getAverageRatingForTool(Tool tool) {
        return getAverageRatingForTool(tool, Feedback.Status.PUBLISHED);
    }

    /** Avg theo trạng thái (NULL coi như PUBLISHED) */
    public double getAverageRatingForTool(Tool tool, Feedback.Status status) {
        Double avg = feedbackRepo.avgRatingByToolAndStatusOrNull(tool, status);
        return avg != null ? avg : 0.0;
    }

    /** Mặc định: total chỉ tính PUBLISHED */
    public long getTotalReviewsForTool(Tool tool) {
        return getTotalReviewsForTool(tool, Feedback.Status.PUBLISHED);
    }

    /** Total theo trạng thái (NULL coi như PUBLISHED) */
    public long getTotalReviewsForTool(Tool tool, Feedback.Status status) {
        Long count = feedbackRepo.countByToolAndStatusOrNull(tool, status);
        return count != null ? count : 0L;
    }
}
