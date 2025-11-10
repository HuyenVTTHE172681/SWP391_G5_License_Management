package swp391.fa25.lms.service.seller;

import org.springframework.stereotype.Service;
import swp391.fa25.lms.model.*;
import swp391.fa25.lms.repository.CustomerOrderRepository;
import swp391.fa25.lms.repository.FeedbackRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.*;

@Service
public class SellerReportService {

    private final CustomerOrderRepository orderRepository;
    private final FeedbackRepository feedbackRepository;

    public SellerReportService(CustomerOrderRepository orderRepository,
                               FeedbackRepository feedbackRepository) {
        this.orderRepository = orderRepository;
        this.feedbackRepository = feedbackRepository;
    }

    /* =====================================================
     * 1️⃣ HÀM LỌC ĐƠN HÀNG THEO SELLER / TOOL / TIME / METHOD
     * ===================================================== */
    private List<CustomerOrder> fetchFilteredOrders(Account seller, Long toolId,
                                                    LocalDate start, LocalDate end,
                                                    String method) {

        List<CustomerOrder> orders;

        // 🔹 Nếu không chọn tool cụ thể → lấy toàn bộ đơn hàng của seller
        if (toolId == null) {
            orders = orderRepository.findByTool_Seller_AccountId(seller.getAccountId());
        } else {
            orders = orderRepository.findByTool_ToolIdAndTool_Seller_AccountId(toolId, seller.getAccountId());
        }

        List<CustomerOrder> filtered = new ArrayList<>();
        for (CustomerOrder order : orders) {
            if (order.getTransaction() == null ||
                    order.getTransaction().getStatus() != WalletTransaction.TransactionStatus.SUCCESS) {
                continue;
            }
            Tool tool = order.getTool();
            if (tool == null || tool.getLoginMethod() == null) continue;
            if (!"all".equalsIgnoreCase(method)) {
                if (!tool.getLoginMethod().name().equalsIgnoreCase(method)) {
                    continue;
                }
            }
            LocalDate created = order.getCreatedAt().toLocalDate();
            if (start != null && created.isBefore(start)) continue;
            if (end != null && created.isAfter(end)) continue;

            filtered.add(order);
        }

        return filtered;
    }
    /* =====================================================
     * 2️⃣ BÁO CÁO TỔNG QUAN: Doanh thu / Lượt mua / Feedback
     * ===================================================== */
    public Map<String, Object> getSummary(Account seller, Long toolId,
                                          LocalDate start, LocalDate end, String method) {

        Map<String, Object> result = new HashMap<>();

        List<CustomerOrder> orders = fetchFilteredOrders(seller, toolId, start, end, method);
        double totalRevenue = orders.stream()
                .mapToDouble(o -> Optional.ofNullable(o.getPrice()).orElse(0.0))
                .sum();
        long totalFeedbacks;
        if (toolId == null) {
            if ("all".equalsIgnoreCase(method)) {
                totalFeedbacks = feedbackRepository.countByTool_Seller(seller);
            } else {
                // Lọc theo method
                totalFeedbacks = feedbackRepository.findAllBySellerId(seller.getAccountId()).stream()
                        .filter(f -> f.getTool() != null
                                && f.getTool().getLoginMethod() != null
                                && f.getTool().getLoginMethod().name().equalsIgnoreCase(method))
                        .count();
            }
        } else {
            totalFeedbacks = feedbackRepository.countByTool_ToolId(toolId);
        }

        result.put("totalRevenue", String.format("%,.0f", totalRevenue));
        result.put("totalOrders", orders.size());
        result.put("totalFeedbacks", totalFeedbacks);

        return result;
    }

    /* =====================================================
     * 3️⃣ BIỂU ĐỒ DOANH THU THEO NGÀY / TUẦN / THÁNG
     * ===================================================== */
    public List<Map<String, Object>> getRevenueChart(Account seller, Long toolId, String filter,
                                                     LocalDate start, LocalDate end, String method) {

        List<CustomerOrder> orders = fetchFilteredOrders(seller, toolId, start, end, method);

        if (orders.isEmpty()) {
            return List.of(Map.of("empty", true));
        }

        Map<String, Double> groupedRevenue = new TreeMap<>();

        for (CustomerOrder order : orders) {
            if (order.getCreatedAt() == null || order.getPrice() == null) continue;

            LocalDateTime created = order.getCreatedAt();
            String key;

            switch (filter.toLowerCase()) {
                case "day" -> key = created.toLocalDate().toString();
                case "week" -> key = created.getYear() + "-W" + created.get(WeekFields.ISO.weekOfWeekBasedYear());
                default -> key = created.getYear() + "-" + String.format("%02d", created.getMonthValue());
            }

            groupedRevenue.merge(key, order.getPrice(), Double::sum);
        }

        List<Map<String, Object>> chartData = new ArrayList<>();
        for (Map.Entry<String, Double> entry : groupedRevenue.entrySet()) {
            chartData.add(Map.of(
                    "period", entry.getKey(),
                    "amount", entry.getValue()
            ));
        }

        return chartData;
    }

    /* =====================================================
     * 4️⃣ BIỂU ĐỒ TỶ LỆ TOOL THEO PHƯƠNG THỨC ĐĂNG NHẬP
     * ===================================================== */
    public List<Map<String, Object>> getLoginMethodChart(Account seller, Long toolId,
                                                         LocalDate start, LocalDate end, String method) {

        // Luôn lấy tất cả phương thức để hiển thị biểu đồ đầy đủ
        List<CustomerOrder> orders = fetchFilteredOrders(seller, toolId, start, end, "all");
        if (orders.isEmpty()) {
            return List.of(Map.of("empty", true));
        }

        Map<String, Long> methodCount = new HashMap<>();

        for (CustomerOrder order : orders) {
            Tool tool = order.getTool();
            if (tool == null || tool.getLoginMethod() == null) continue;
            String login = tool.getLoginMethod().name();
            methodCount.put(login, methodCount.getOrDefault(login, 0L) + 1);
        }

        List<Map<String, Object>> chartData = new ArrayList<>();
        for (Map.Entry<String, Long> entry : methodCount.entrySet()) {
            chartData.add(Map.of(
                    "method", entry.getKey(),
                    "count", entry.getValue()
            ));
        }

        return chartData;
    }

    /* =====================================================
     * 5️⃣ FEEDBACK & ĐÁNH GIÁ (lọc theo method)
     * ===================================================== */
    public Map<String, Object> getFeedbackSummary(Account seller, Long toolId, String method) {

        Map<String, Object> result = new HashMap<>();

        List<Feedback> feedbacks;
        if (toolId == null) {
            feedbacks = feedbackRepository.findAllBySellerId(seller.getAccountId());
        } else {
            feedbacks = feedbackRepository.findAllBySellerIdAndToolId(seller.getAccountId(), toolId);
        }

        // 🔹 Lọc theo phương thức đăng nhập (TOKEN / USER_PASSWORD / ALL)
        if (method != null && !"all".equalsIgnoreCase(method)) {
            feedbacks = feedbacks.stream()
                    .filter(f -> f.getTool() != null
                            && f.getTool().getLoginMethod() != null
                            && f.getTool().getLoginMethod().name().equalsIgnoreCase(method))
                    .toList();
        }

        int totalFeedbacks = feedbacks.size();
        double averageRating = feedbacks.stream()
                .mapToInt(Feedback::getRating)
                .average()
                .orElse(0.0);

        Map<Integer, Long> countByStar = new TreeMap<>(Comparator.reverseOrder());
        for (int i = 1; i <= 5; i++) countByStar.put(i, 0L);

        for (Feedback f : feedbacks) {
            countByStar.merge(f.getRating(), 1L, Long::sum);
        }

        List<Map<String, Object>> details = new ArrayList<>();
        for (Map.Entry<Integer, Long> entry : countByStar.entrySet()) {
            details.add(Map.of(
                    "star", entry.getKey(),
                    "count", entry.getValue()
            ));
        }

        result.put("totalFeedbacks", totalFeedbacks);
        result.put("averageRating", String.format("%.1f", averageRating));
        result.put("details", details);

        return result;
    }
}
