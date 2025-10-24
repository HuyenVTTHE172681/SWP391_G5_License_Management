package swp391.fa25.lms.service.customer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import swp391.fa25.lms.model.Feedback;
import swp391.fa25.lms.model.Tool;
import swp391.fa25.lms.repository.FeedbackRepository;
import swp391.fa25.lms.repository.ToolRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ToolService {
    @Autowired
    private ToolRepository toolRepo;

    @Autowired
    private FeedbackRepository feedbackRepo;

    /**
     * Tìm tools (search + paging) – sửa lại để dùng Page (không load toàn bộ list)
     */
    public Page<Tool> searchAndFilterTools(String keyword, Long categoryId, String dateFilter, int page, int size) {
        // Lấy toàn bộ tools hoặc lấy theo tên
        List<Tool> tools;
        if (keyword != null && !keyword.isEmpty()) {
            tools = toolRepo.findByToolNameContainingIgnoreCase(keyword);
        } else {
            tools = toolRepo.findAll();
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
                    tools = tools.stream()
                            .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                            .toList();
                    break;
                case "2": // 30 ngày qua
                    tools = tools.stream()
                            .filter(t -> t.getCreatedAt().isAfter(now.minusDays(30)))
                            .toList();
                    break;
                case "3": // 3 tháng qua
                    tools = tools.stream()
                            .filter(t -> t.getCreatedAt().isAfter(now.minusMonths(3)))
                            .toList();
                    break;
            }
        }

        // Tính rating trung bình & số feedback cho từng tool
        tools.forEach(tool -> {
            Double avgRating = feedbackRepo.findAverageRatingByTool(tool.getToolId());
            Long totalReviews = feedbackRepo.countByTool(tool);

            tool.setAverageRating(avgRating != null ? avgRating : 0.0);
            tool.setTotalReviews(totalReviews != null ? totalReviews : 0L);
        });

        // Phân trang
        int start = page * size;
        int end = Math.min(start + size, tools.size());
        List<Tool> pagedList = tools.subList(start < tools.size() ? start : 0, end);

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
        return feedbackRepo.findByTool(tool, pageable);
    }

    /**
     * Tính rating trung bình (null safe)
     */
    public double getAverageRatingForTool(Tool tool) {
        Double avg = feedbackRepo.findAverageRatingByTool(tool.getToolId());
        return avg != null ? avg : 0.0;
    }

    public long getTotalReviewsForTool(Tool tool) {
        Long count = feedbackRepo.countByToolId(tool.getToolId());
        return count != null ? count : 0L;
    }
}
