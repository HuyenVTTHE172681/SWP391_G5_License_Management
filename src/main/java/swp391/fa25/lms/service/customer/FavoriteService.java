package swp391.fa25.lms.service.customer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.Favorite;
import swp391.fa25.lms.model.Tool;
import swp391.fa25.lms.repository.AccountRepository;
import swp391.fa25.lms.repository.FavoriteRepository;
import swp391.fa25.lms.repository.FeedbackRepository;
import swp391.fa25.lms.repository.ToolRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FavoriteService {

    @Autowired
    private FavoriteRepository favoriteRepository;
    @Autowired
    private ToolRepository toolRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private FeedbackRepository feedbackRepo;

    /**
     * Toggle favorite: nếu đã yêu thích -> xóa, nếu chưa -> thêm
     * @return true nếu thêm, false nếu xóa
     */
    public boolean toggleFavorite(Account account, Tool tool) {
        // Tìm xem đã favorite chưa
        return favoriteRepository.findByAccountAndTool(account, tool)
                .map(fav -> {
                    favoriteRepository.delete(fav);  // nếu tồn tại -> xóa
                    return false;
                })
                .orElseGet(() -> {
                    Favorite newFav = new Favorite();
                    newFav.setAccount(account);
                    newFav.setTool(tool);
                    favoriteRepository.save(newFav); // nếu chưa tồn tại -> thêm
                    return true;
                });
    }

    /**
     * Lấy danh sách tool đã yêu thích của account
     */
    public List<Tool> getFavoriteTools(Account account) {
        return favoriteRepository.findByAccount(account).stream()
                .map(Favorite::getTool)
                .toList();
    }

    /**
     * Đếm số favorite của account
     */
    public long countFavoritesByAccount(Account account) {
        return favoriteRepository.countByAccount(account);
    }

    /**
     * Count trực tiếp by ID
     */
    public long countByAccountId(Long accountId) {
        if (accountId == null) return 0L;
        return favoriteRepository.countByAccountId(accountId);
    }

    /**
     * Lấy Set tool IDs yêu thích của account
     */
    public Set<Long> getFavoriteToolIds(Account account) {
        return favoriteRepository.findByAccount(account).stream()
                .map(fav -> fav.getTool().getToolId())
                .collect(Collectors.toSet());
    }

    /**
     * Fix: Lấy list Tool favorite của account (dùng ID để tránh dummy Account)
     */
    public List<Tool> getFavoritesByAccount(Long accountId) {
        // Thêm method findByAccountId ở Repo (xem dưới)
        return favoriteRepository.findByAccountId(accountId).stream()
                .map(Favorite::getTool)
                .toList();
    }

    /**
     * Filter trên list favorites
     * @param favorites List<Tool> đã favorite
     * @param keyword Tool name hoặc seller name
     * @param categoryId Category filter
     * @param dateFilter Ngày đăng
     * @param priceFilter Giá
     * @param ratingFilter Số sao
     * @param page Trang
     * @param size Số item mỗi trang
     * @return Page<Tool> đã filter/paginate
     */
    public Page<Tool> searchAndFilterFavorites(List<Tool> favorites, String keyword, Long categoryId, String dateFilter,
                                               String priceFilter, Integer ratingFilter, int page, int size) {

        List<Tool> filteredTools = new ArrayList<>(favorites);

        // Search keyword
        if (keyword != null && !keyword.isEmpty()) {
            String kwLower = keyword.toLowerCase();
            filteredTools = filteredTools.stream()
                    .filter(t -> t.getToolName().toLowerCase().contains(kwLower)
                            || t.getSeller().getFullName().toLowerCase().contains(kwLower))
                    .toList();
        }

        // Filter category
        if (categoryId != null && categoryId > 0) {
            filteredTools = filteredTools.stream()
                    .filter(t -> t.getCategory() != null && t.getCategory().getCategoryId().equals(categoryId))
                    .toList();
        }

        // Filter date
        if (dateFilter != null && !dateFilter.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            switch (dateFilter) {
                case "1":  // Mới nhất
                    filteredTools = filteredTools.stream()
                            .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                            .toList();
                    break;
                case "2":  // 30 ngày
                    filteredTools = filteredTools.stream()
                            .filter(t -> t.getCreatedAt().isAfter(now.minusDays(30)))
                            .toList();
                    break;
                case "3":  // 3 tháng
                    filteredTools = filteredTools.stream()
                            .filter(t -> t.getCreatedAt().isAfter(now.minusMonths(3)))
                            .toList();
                    break;
            }
        }

        // Tính minPrice, maxPrice, avgRating, totalReviews (giống ToolService)
        filteredTools.forEach(tool -> {
            // Giá (từ licenses)
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

            // Set isFavorite = true (vì là favorites)
            tool.setIsFavorite(true);
        });

        // Filter price
        if (priceFilter != null && !priceFilter.equals("all")) {
            filteredTools = filteredTools.stream().filter(t -> {
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

        // Filter rating
        if (ratingFilter != null && ratingFilter > 0) {
            filteredTools = filteredTools.stream()
                    .filter(t -> t.getAverageRating() >= ratingFilter)
                    .toList();
        }

        // Pagination
        int start = page * size;
        int end = Math.min(start + size, filteredTools.size());
        List<Tool> pagedList = filteredTools.subList(Math.min(start, filteredTools.size()), end);

        return new PageImpl<>(pagedList, PageRequest.of(page, size), filteredTools.size());
    }

}
