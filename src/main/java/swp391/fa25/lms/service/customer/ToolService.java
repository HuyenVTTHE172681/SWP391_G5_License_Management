package swp391.fa25.lms.service.customer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.Feedback;
import swp391.fa25.lms.model.License;
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
    @Autowired
    private ToolRepository toolRepo;

    @Autowired
    private FeedbackRepository feedbackRepo;

    @Autowired
    private FavoriteService favoriteService;

    /**
     * Search + Filter nâng cao
     * @param keyword Tool name hoặc seller name
     * @param categoryId Category filter
     * @param dateFilter Ngày đăng
     * @param priceFilter Giá: "all", "under100k", "100k-500k", "500k-1m", "above1m"
     * @param ratingFilter Số sao tối thiểu
     * @param account Account hiện tại (null nếu chưa login)
     * @param page Trang
     * @param size Số item mỗi trang
     * @return Page<Tool>
     */
    public Page<Tool> searchAndFilterTools(String keyword, Long categoryId, String dateFilter,
                                           String priceFilter, Integer ratingFilter,
                                           Account account, int page, int size) {

        List<Tool> tools = toolRepo.findAllPublishedAndSellerActive(); // Lấy tất cả, filter ở memory

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
                case "1": // Mới nhất
                    tools = tools.stream().sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt())).toList();
                    break;
                case "2": // 30 ngày
                    tools = tools.stream().filter(t -> t.getCreatedAt().isAfter(now.minusDays(30))).toList();
                    break;
                case "3": // 3 tháng
                    tools = tools.stream().filter(t -> t.getCreatedAt().isAfter(now.minusMonths(3))).toList();
                    break;
            }
        }

        // Tính minPrice, maxPrice, avgRating, totalReviews
        tools.forEach(tool -> {
            // Giá
            if (tool.getLicenses() != null && !tool.getLicenses().isEmpty()) {
                BigDecimal min = tool.getLicenses().stream()
                        .map(l -> BigDecimal.valueOf(l.getPrice()))
                        .min(BigDecimal::compareTo)
                        .orElse(BigDecimal.ZERO);
                BigDecimal max = tool.getLicenses().stream()
                        .map(l -> BigDecimal.valueOf(l.getPrice()))
                        .max(BigDecimal::compareTo)
                        .orElse(BigDecimal.ZERO);
                tool.setMinPrice(min);
                tool.setMaxPrice(max);
            } else {
                tool.setMinPrice(BigDecimal.ZERO);
                tool.setMaxPrice(BigDecimal.ZERO);
            }

            // Rating
            Long avg = feedbackRepo.findAverageRatingByToolByStatus(tool.getToolId());
            Long total = feedbackRepo.countByToolIdByStatus(tool.getToolId());
            tool.setAverageRating(avg != null ? avg : 0.0);
            tool.setTotalReviews(total != null ? total : 0L);
        });

        // Filter theo price
        if (priceFilter != null && !priceFilter.equals("all")) {
            tools = tools.stream().filter(t -> {
                BigDecimal min = t.getMinPrice() != null ? t.getMinPrice() : BigDecimal.ZERO;
                switch (priceFilter) {
                    case "under100k": return min.compareTo(BigDecimal.valueOf(100_000)) < 0;
                    case "100k-500k": return min.compareTo(BigDecimal.valueOf(100_000)) >= 0
                            && min.compareTo(BigDecimal.valueOf(500_000)) <= 0;
                    case "500k-1m": return min.compareTo(BigDecimal.valueOf(500_000)) > 0
                            && min.compareTo(BigDecimal.valueOf(1_000_000)) <= 0;
                    case "above1m": return min.compareTo(BigDecimal.valueOf(1_000_000)) > 0;
                }
                return true;
            }).toList();
        }

        // Filter theo rating
        if (ratingFilter != null && ratingFilter > 0) {
            tools = tools.stream()
                    .filter(t -> t.getAverageRating() >= ratingFilter)
                    .toList();
        }

        // Set isFavorite cho mỗi tool (nếu có account)
        if (account != null) {
            Set<Long> favoriteToolIds = favoriteService.getFavoriteToolIds(account);
            tools.forEach(tool -> tool.setIsFavorite(favoriteToolIds.contains(tool.getToolId())));
        } else {
            tools.forEach(tool -> tool.setIsFavorite(false));  // Chưa login: không favorite
        }

        // Pagination
        int start = page * size;
        int end = Math.min(start + size, tools.size());
        List<Tool> pagedList = tools.subList(Math.min(start, tools.size()), end);

        return new PageImpl<>(pagedList, PageRequest.of(page, size), tools.size());
    }

    /**
     * Lấy tool theo id, status = PUBLISHED
     */
    public Optional<Tool> findPublishedToolById(Long id) {
        return toolRepo.findByToolIdAndStatus(id, Tool.Status.PUBLISHED);
    }

    /**
     * Lấy feedback tool
     */
    public Page<Feedback> getFeedbackPageForTool(Tool tool, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return feedbackRepo.findByToolAndStatus(
                tool,
                Feedback.Status.PUBLISHED,
                pageable);
    }

    /**
     * Tính rating trung bình (chỉ PUBLISHED)
     */
    public double getAverageRatingForTool(Tool tool) {
        if (tool == null || tool.getToolId() == null) return 0.0;
        Long avg = feedbackRepo.findAverageRatingByToolByStatus(tool.getToolId());  // Không pass status
        return avg != null ? avg : 0.0;
    }

    /**
     * Tổng số review (chỉ PUBLISHED)
     */
    public long getTotalReviewsForTool(Tool tool) {
        if (tool == null || tool.getToolId() == null) return 0L;
        Long count = feedbackRepo.countByToolIdByStatus(tool.getToolId());  // Không pass status
        return count != null ? count : 0L;
    }

    /**
     * Lấy tool theo id
     */
    public Optional<Tool> findById(Long id) {
        return toolRepo.findById(id);
    }

    public Tool getToolById(Long toolId) {
        return toolRepo.findById(toolId).orElse(null);
    }

}
