package swp391.fa25.lms.service.manager;

import org.springframework.stereotype.Service;
import swp391.fa25.lms.model.SellerSubscription;
import swp391.fa25.lms.model.ToolReport;
import swp391.fa25.lms.repository.SellerSubscriptionRepository;
import swp391.fa25.lms.repository.ToolReportRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.*;

/**
 * Service cho màn Manager Dashboard
 * Thống kê:
 *  1️⃣ Seller Package Purchases
 *  2️⃣ Tool Reports
 */
@Service
public class ManagerDashboardService {

    private final SellerSubscriptionRepository subscriptionRepository;
    private final ToolReportRepository toolReportRepository;

    public ManagerDashboardService(SellerSubscriptionRepository subscriptionRepository,
                                   ToolReportRepository toolReportRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.toolReportRepository = toolReportRepository;
    }

    // ==========================================================
    // 🔹 Helper: kiểm tra ngày có nằm trong khoảng lọc không
    // ==========================================================
    private boolean isInDateRange(LocalDate date, LocalDate start, LocalDate end) {
        if (date == null) return false;
        if (start != null && date.isBefore(start)) return false;
        if (end != null && date.isAfter(end)) return false;
        return true;
    }

    // ==========================================================
    // 🔹 Helper: tạo key theo kiểu (day / week / month)
    // ==========================================================
    private String createPeriodKey(LocalDateTime time, String periodType) {
        if (time == null) return "Unknown";
        if (periodType == null) periodType = "month";
        periodType = periodType.toLowerCase();

        switch (periodType) {
            case "day":
                return time.toLocalDate().toString();
            case "week":
                int week = time.get(WeekFields.ISO.weekOfWeekBasedYear());
                return time.getYear() + "-W" + week;
            default:
                return time.getYear() + "-" + String.format("%02d", time.getMonthValue());
        }
    }

    // ==========================================================
    // 1️⃣ Biểu đồ: Seller Package Purchases
    // ==========================================================
    public List<Map<String, Object>> getSellerPackageChart(String periodType,
                                                           LocalDate start,
                                                           LocalDate end) {

        System.out.println("\n=======================");
        System.out.println("📦 [DEBUG] getSellerPackageChart()");
        System.out.println("PeriodType = " + periodType + ", Start = " + start + ", End = " + end);

        List<SellerSubscription> subscriptions = subscriptionRepository.findAll();
        System.out.println("Total subscriptions found: " + (subscriptions == null ? 0 : subscriptions.size()));

        if (subscriptions == null || subscriptions.isEmpty()) {
            System.out.println("⚠️ No SellerSubscription records found in DB!");
            return Collections.emptyList();
        }

        Map<String, Long> grouped = new TreeMap<>();

        for (SellerSubscription sub : subscriptions) {
            LocalDateTime startDateTime = sub.getStartDate();
            if (startDateTime == null) {
                System.out.println("⚠️ Skipped record: startDate is null → " + sub);
                continue;
            }

            LocalDate date = startDateTime.toLocalDate();
            if (!isInDateRange(date, start, end)) {
                System.out.println("⏩ Skipped (out of range): " + date);
                continue;
            }

            String key = createPeriodKey(startDateTime, periodType);
            grouped.put(key, grouped.getOrDefault(key, 0L) + 1);
            System.out.println("✅ Counted record at key = " + key);
        }

        // Convert Map -> List
        List<Map<String, Object>> chartData = new ArrayList<>();
        grouped.forEach((k, v) -> {
            Map<String, Object> item = new HashMap<>();
            item.put("period", k);
            item.put("count", v);
            chartData.add(item);
        });

        System.out.println("📊 Final grouped data: " + grouped);
        System.out.println("=======================\n");

        return chartData;
    }

    // ==========================================================
    // 2️⃣ Biểu đồ: Tool Reports
    // ==========================================================
    public List<Map<String, Object>> getToolReportChart(String periodType,
                                                        LocalDate start,
                                                        LocalDate end) {

        System.out.println("PeriodType = " + periodType + ", Start = " + start + ", End = " + end);

        List<ToolReport> reports = toolReportRepository.findAll();
        System.out.println("Total reports found: " + (reports == null ? 0 : reports.size()));

        if (reports == null || reports.isEmpty()) {
            System.out.println("⚠️ No ToolReport records found in DB!");
            return Collections.emptyList();
        }

        Map<String, Long> grouped = new TreeMap<>();

        for (ToolReport report : reports) {
            LocalDate reportedDate = report.getReportedAt();
            if (reportedDate == null) {
                System.out.println("⚠️ Skipped record: reportedAt is null → " + report);
                continue;
            }

            if (!isInDateRange(reportedDate, start, end)) {
                System.out.println("⏩ Skipped (out of range): " + reportedDate);
                continue;
            }

            String key = createPeriodKey(reportedDate.atStartOfDay(), periodType);
            grouped.put(key, grouped.getOrDefault(key, 0L) + 1);
            System.out.println("✅ Counted report at key = " + key);
        }

        List<Map<String, Object>> chartData = new ArrayList<>();
        grouped.forEach((k, v) -> {
            Map<String, Object> item = new HashMap<>();
            item.put("period", k);
            item.put("count", v);
            chartData.add(item);
        });

        System.out.println("📊 Final grouped data: " + grouped);
        System.out.println("=======================\n");

        return chartData;
    }

    // ==========================================================
    // 3️⃣ Biểu đồ: Seller Status (Active / Expired)
    // ==========================================================
    public List<Map<String, Object>> getSellerStatusChart() {

        List<SellerSubscription> subs = subscriptionRepository.findAll();
        System.out.println("Total subscriptions found: " + (subs == null ? 0 : subs.size()));

        if (subs == null || subs.isEmpty()) {
            System.out.println("⚠️ No SellerSubscription records found!");
            return Collections.emptyList();
        }

        long activeCount = subs.stream().filter(SellerSubscription::isActive).count();
        long expiredCount = subs.size() - activeCount;

        System.out.println("Active count = " + activeCount + ", Expired count = " + expiredCount);
        System.out.println("=======================\n");

        List<Map<String, Object>> chart = new ArrayList<>();

        Map<String, Object> active = new HashMap<>();
        active.put("status", "Active");
        active.put("count", activeCount);
        chart.add(active);

        Map<String, Object> expired = new HashMap<>();
        expired.put("status", "Expired");
        expired.put("count", expiredCount);
        chart.add(expired);

        return chart;
    }
}
