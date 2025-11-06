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
     * 1️⃣ HÀM LỌC ĐƠN HÀNG GỐC
     * ===================================================== */
    private List<CustomerOrder> fetchFilteredOrders(Account seller, Long toolId,
                                                    LocalDate start, LocalDate end,
                                                    String method) {

        // Bước 1: Lấy danh sách đơn hàng
        List<CustomerOrder> orders;
        if (toolId == null) {
            orders = orderRepository.findByTool_Seller_AccountId(seller.getAccountId());
        } else {
            orders = orderRepository.findByTool_ToolIdAndTool_Seller_AccountId(toolId, seller.getAccountId());
        }

        // Bước 2: Lọc theo điều kiện
        List<CustomerOrder> filteredOrders = new ArrayList<>();
        for (CustomerOrder order : orders) {

            // Chỉ lấy đơn đã thanh toán thành công
            if (order.getTransaction() == null ||
                    order.getTransaction().getStatus() != WalletTransaction.TransactionStatus.SUCCESS) {
                continue;
            }

            // Chỉ lấy tool có login method hợp lệ
            if (order.getTool() == null || order.getTool().getLoginMethod() == null) {
                continue;
            }

            // Lọc theo phương thức đăng nhập (TOKEN / USER_PASSWORD / ALL)
            if (!"all".equalsIgnoreCase(method)) {
                if (!order.getTool().getLoginMethod().name().equalsIgnoreCase(method)) {
                    continue;
                }
            }

            // Lọc theo khoảng thời gian
            LocalDate createdDate = order.getCreatedAt().toLocalDate();
            if (start != null && createdDate.isBefore(start)) continue;
            if (end != null && createdDate.isAfter(end)) continue;

            filteredOrders.add(order);
        }

        return filteredOrders;
    }

    /* =====================================================
     * 2️⃣ TỔNG QUAN DOANH THU
     * ===================================================== */
    public Map<String, Object> getSummary(Account seller, Long toolId,
                                          LocalDate start, LocalDate end, String method) {

        Map<String, Object> result = new HashMap<>();
        List<CustomerOrder> orders = fetchFilteredOrders(seller, toolId, start, end, method);

        double totalRevenue = 0;
        for (CustomerOrder order : orders) {
            Double price = order.getPrice();
            if (price != null) {
                totalRevenue += price;
            }
        }

        long totalFeedbacks;
        if (toolId == null) {
            totalFeedbacks = feedbackRepository.countByTool_Seller(seller);
        } else {
            totalFeedbacks = feedbackRepository.countByTool_ToolId(toolId);
        }

        result.put("totalRevenue", String.format("%,.0f", totalRevenue));
        result.put("totalOrders", orders.size());
        result.put("totalFeedbacks", totalFeedbacks);

        return result;
    }

    /* =====================================================
     * 3️⃣ BIỂU ĐỒ DOANH THU
     * ===================================================== */
    public List<Map<String, Object>> getRevenueChart(Account seller, Long toolId, String filter,
                                                     LocalDate start, LocalDate end, String method) {

        List<CustomerOrder> orders = fetchFilteredOrders(seller, toolId, start, end, method);
        if (orders.isEmpty()) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("empty", true);
            return List.of(empty);
        }

        Map<String, Double> groupedRevenue = new TreeMap<>();

        for (CustomerOrder order : orders) {
            if (order.getCreatedAt() == null || order.getPrice() == null) continue;

            LocalDateTime created = order.getCreatedAt();
            String key;

            if ("day".equalsIgnoreCase(filter)) {
                key = created.toLocalDate().toString();
            } else if ("week".equalsIgnoreCase(filter)) {
                key = created.getYear() + "-W" + created.get(WeekFields.ISO.weekOfWeekBasedYear());
            } else {
                key = created.getYear() + "-" + String.format("%02d", created.getMonthValue());
            }

            groupedRevenue.merge(key, order.getPrice(), Double::sum);
        }

        List<Map<String, Object>> chartData = new ArrayList<>();
        for (Map.Entry<String, Double> entry : groupedRevenue.entrySet()) {
            Map<String, Object> point = new HashMap<>();
            point.put("period", entry.getKey());
            point.put("amount", entry.getValue());
            chartData.add(point);
        }

        return chartData;
    }

    /* =====================================================
     * 4️⃣ BIỂU ĐỒ PHƯƠNG THỨC MUA TOOL
     * ===================================================== */
    public List<Map<String, Object>> getLoginMethodChart(Account seller, Long toolId,
                                                         LocalDate start, LocalDate end, String method) {

        List<CustomerOrder> orders = fetchFilteredOrders(seller, toolId, start, end, "all");
        if (orders.isEmpty()) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("empty", true);
            return List.of(empty);
        }

        Map<String, Long> methodCount = new HashMap<>();

        for (CustomerOrder order : orders) {
            if (order.getTool() == null || order.getTool().getLoginMethod() == null) continue;
            String loginMethod = order.getTool().getLoginMethod().name();
            methodCount.put(loginMethod, methodCount.getOrDefault(loginMethod, 0L) + 1);
        }

        List<Map<String, Object>> chartData = new ArrayList<>();
        for (Map.Entry<String, Long> entry : methodCount.entrySet()) {
            Map<String, Object> point = new HashMap<>();
            point.put("method", entry.getKey());
            point.put("count", entry.getValue());
            chartData.add(point);
        }

        return chartData;
    }

    /* =====================================================
     * 5️⃣ FEEDBACK & ĐÁNH GIÁ
     * ===================================================== */
    public Map<String, Object> getFeedbackSummary(Account seller, Long toolId) {

        Map<String, Object> result = new HashMap<>();

        List<Feedback> feedbacks;
        if (toolId == null) {
            feedbacks = feedbackRepository.findAllBySellerId(seller.getAccountId());
        } else {
            feedbacks = feedbackRepository.findAllBySellerIdAndToolId(seller.getAccountId(), toolId);
        }

        int totalFeedbacks = feedbacks.size();
        double averageRating = feedbacks.stream()
                .mapToInt(Feedback::getRating)
                .average()
                .orElse(0.0);

        Map<Integer, Long> countByStar = new TreeMap<>(Comparator.reverseOrder());
        for (int i = 1; i <= 5; i++) {
            countByStar.put(i, 0L);
        }

        for (Feedback f : feedbacks) {
            int star = f.getRating();
            countByStar.put(star, countByStar.get(star) + 1);
        }

        List<Map<String, Object>> details = new ArrayList<>();
        for (Map.Entry<Integer, Long> entry : countByStar.entrySet()) {
            Map<String, Object> detail = new HashMap<>();
            detail.put("star", entry.getKey());
            detail.put("count", entry.getValue());
            details.add(detail);
        }

        result.put("totalFeedbacks", totalFeedbacks);
        result.put("averageRating", String.format("%.1f", averageRating));
        result.put("details", details);

        return result;
    }
}
