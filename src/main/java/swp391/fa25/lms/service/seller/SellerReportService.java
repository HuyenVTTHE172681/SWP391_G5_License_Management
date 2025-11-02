package swp391.fa25.lms.service.seller;

import org.springframework.stereotype.Service;
import swp391.fa25.lms.model.*;
import swp391.fa25.lms.repository.CustomerOrderRepository;
import swp391.fa25.lms.repository.FeedbackRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SellerReportService {

    private final CustomerOrderRepository orderRepository;
    private final FeedbackRepository feedbackRepository;

    public SellerReportService(CustomerOrderRepository orderRepository,
                               FeedbackRepository feedbackRepository) {
        this.orderRepository = orderRepository;
        this.feedbackRepository = feedbackRepository;
    }

    // ===================== LỌC CHUNG =====================
    private List<CustomerOrder> fetchFilteredOrders(Account seller, Long toolId,
                                                    LocalDate start, LocalDate end,
                                                    String method) {
        List<CustomerOrder> orders = (toolId == null)
                ? orderRepository.findByTool_Seller_AccountId(seller.getAccountId())
                : orderRepository.findByTool_ToolIdAndTool_Seller_AccountId(toolId, seller.getAccountId());

        return orders.stream()
                .filter(o -> o.getTransaction() != null
                        && o.getTransaction().getStatus() == WalletTransaction.TransactionStatus.SUCCESS)
                .filter(o -> o.getTool() != null && o.getTool().getLoginMethod() != null)
                .filter(o -> {
                    if ("all".equalsIgnoreCase(method)) return true;
                    return o.getTool().getLoginMethod().name().equalsIgnoreCase(method);
                })
                .filter(o -> {
                    if (start != null && o.getCreatedAt().toLocalDate().isBefore(start)) return false;
                    if (end != null && o.getCreatedAt().toLocalDate().isAfter(end)) return false;
                    return true;
                })
                .collect(Collectors.toList());
    }

    // ===================== TỔNG QUAN =====================
    public Map<String, Object> getSummary(Account seller, Long toolId,
                                          LocalDate start, LocalDate end, String method) {
        Map<String, Object> map = new HashMap<>();
        List<CustomerOrder> orders = fetchFilteredOrders(seller, toolId, start, end, method);

        double totalRevenue = orders.stream()
                .mapToDouble(o -> Optional.ofNullable(o.getPrice()).orElse(0.0))
                .sum();

        map.put("totalRevenue", String.format("%,.0f", totalRevenue));
        map.put("totalOrders", orders.size());

        long totalFeedbacks = (toolId == null)
                ? feedbackRepository.countByTool_Seller(seller)
                : feedbackRepository.countByTool_ToolId(toolId);
        map.put("totalFeedbacks", totalFeedbacks);

        return map;
    }

    // ===================== BIỂU ĐỒ DOANH THU =====================
    public List<Map<String, Object>> getRevenueChart(Account seller, Long toolId, String filter,
                                                     LocalDate start, LocalDate end, String method) {
        List<CustomerOrder> orders = fetchFilteredOrders(seller, toolId, start, end, method);

        if (orders.isEmpty()) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("empty", true);
            return List.of(empty);
        }

        Map<String, Double> grouped = new TreeMap<>();
        for (CustomerOrder o : orders) {
            if (o.getCreatedAt() == null || o.getPrice() == null) continue;
            String key;
            LocalDateTime date = o.getCreatedAt();
            switch (filter) {
                case "day" -> key = date.toLocalDate().toString();
                case "week" -> key = date.getYear() + "-W" + date.get(WeekFields.ISO.weekOfWeekBasedYear());
                default -> key = date.getYear() + "-" + String.format("%02d", date.getMonthValue());
            }
            grouped.merge(key, o.getPrice(), Double::sum);
        }

        return grouped.entrySet().stream()
                .map(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("period", e.getKey());
                    m.put("amount", e.getValue());
                    return m;
                })
                .collect(Collectors.toList());
    }

        // ===================== BIỂU ĐỒ PHƯƠNG THỨC MUA TOOL =====================
    public List<Map<String, Object>> getLoginMethodChart(Account seller, Long toolId,
                                                         LocalDate start, LocalDate end, String method) {
        List<CustomerOrder> orders = fetchFilteredOrders(seller, toolId, start, end, "all");

        if (orders.isEmpty()) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("empty", true);
            return List.of(empty);
        }

        Map<String, Long> count = orders.stream()
                .filter(o -> o.getTool() != null && o.getTool().getLoginMethod() != null)
                .collect(Collectors.groupingBy(
                        o -> o.getTool().getLoginMethod().name(),
                        Collectors.counting()
                ));

        return count.entrySet().stream()
                .map(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("method", e.getKey());
                    m.put("count", e.getValue());
                    return m;
                })
                .collect(Collectors.toList());
    }
        // ===================== BIỂU ĐỒ FEEDBACK =====================
    public Map<String, Object> getFeedbackSummary(Account seller, Long toolId) {
        Map<String, Object> map = new HashMap<>();
        List<Feedback> feedbacks = (toolId == null)
                ? feedbackRepository.findAllBySellerId(seller.getAccountId())
                : feedbackRepository.findAllBySellerIdAndToolId(seller.getAccountId(), toolId);

        int total = feedbacks.size();
        map.put("totalFeedbacks", total);
        double avg = feedbacks.stream().mapToInt(Feedback::getRating).average().orElse(0.0);
        map.put("averageRating", String.format("%.1f", avg));

        Map<Integer, Long> countByStar = new HashMap<>();
        for (int i = 1; i <= 5; i++) countByStar.put(i, 0L);
        feedbacks.forEach(f -> countByStar.merge(f.getRating(), 1L, Long::sum));

        List<Map<String, Object>> details = countByStar.entrySet().stream()
                .sorted(Map.Entry.<Integer, Long>comparingByKey().reversed())
                .map(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("star", e.getKey());
                    m.put("count", e.getValue());
                    return m;
                })
                .collect(Collectors.toList());
        map.put("details", details);
        return map;
    }
}
