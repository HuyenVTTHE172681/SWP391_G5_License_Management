package swp391.fa25.lms.controller.seller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.CustomerOrder;
import swp391.fa25.lms.model.Tool;
import swp391.fa25.lms.repository.CustomerOrderRepository;
import swp391.fa25.lms.service.seller.ToolService;

import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.*;

@Controller
@RequestMapping("/seller")
public class SellerNavigationController {

    @Autowired
    private ToolService toolService;

    @Autowired
    private CustomerOrderRepository orderRepository;
    // 🧑‍💼 Trang View Profile
    @GetMapping("/seller/profile")
    public String goToProfile() {
        // Trả về tên trang (template) — dev frontend sẽ tạo file "profile.html"
        return "seller/profile";
    }

    // 💬 Trang View Feedback
//    @GetMapping("/seller/feedback")
//    public String goToFeedback() {
//        // Trả về template "feedback.html" để team frontend xử lý sau
//        return "seller/feedback";
//    }
    // ==============================
    // 🟢 1. Hiển thị trang report
    // ==============================
    @GetMapping("/tools/sales-report")
    public String showSalesReport(
            @RequestParam(value = "toolId", required = false) Long toolId,
            HttpServletRequest request,
            Model model) {

        Account seller = (Account) request.getSession().getAttribute("loggedInAccount");
        if (seller == null) throw new RuntimeException("Bạn chưa đăng nhập.");

        if (toolId != null) {
            Tool tool = toolService.getToolById(toolId);
            if (tool == null || !tool.getSeller().getAccountId().equals(seller.getAccountId())) {
                throw new RuntimeException("Không có quyền xem báo cáo cho tool này.");
            }
            model.addAttribute("tool", tool);
            model.addAttribute("toolName", tool.getToolName());
        } else {
            model.addAttribute("toolName", "Tất cả tool");
        }

        return "seller/sales-report";
    }

    // ==============================
    // 🟣 2. API trả dữ liệu doanh thu JSON
    // ==============================
    @GetMapping("/tools/sales-report/data")
    @ResponseBody
    public Map<String, Object> getSalesData(
            @RequestParam(value = "toolId", required = false) Long toolId,
            @RequestParam(value = "filter", defaultValue = "month") String filter,
            HttpServletRequest request) {

        Account seller = (Account) request.getSession().getAttribute("loggedInAccount");
        if (seller == null) throw new RuntimeException("Bạn chưa đăng nhập.");

        // Lấy toàn bộ đơn hàng của seller (hoặc tool cụ thể)
        List<CustomerOrder> orders;
        if (toolId != null) {
            orders = orderRepository.findByTool_ToolIdAndTool_Seller_AccountId(toolId, seller.getAccountId());
        } else {
            orders = orderRepository.findByTool_Seller_AccountId(seller.getAccountId());
        }

        // Gom nhóm theo ngày / tuần / tháng
        Map<String, Double> salesMap = new TreeMap<>();

        for (CustomerOrder order : orders) {
            if (order.getCreatedAt() == null || order.getPrice() == null) continue;

            String key;
            LocalDateTime date = order.getCreatedAt();

            switch (filter) {
                case "day" -> key = date.toLocalDate().toString(); // YYYY-MM-DD
                case "week" -> key = date.getYear() + "-W" + date.get(WeekFields.ISO.weekOfWeekBasedYear());
                default -> key = date.getYear() + "-" + String.format("%02d", date.getMonthValue()); // YYYY-MM
            }

            salesMap.merge(key, order.getPrice(), Double::sum);
        }

        // Trả về JSON cho Chart.js
        Map<String, Object> result = new HashMap<>();
        result.put("labels", new ArrayList<>(salesMap.keySet()));
        result.put("data", new ArrayList<>(salesMap.values()));
        result.put("filter", filter);
        result.put("toolId", toolId);

        return result;
    }
}

