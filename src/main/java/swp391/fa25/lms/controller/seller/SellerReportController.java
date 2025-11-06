package swp391.fa25.lms.controller.seller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.service.seller.SellerReportService;
import swp391.fa25.lms.service.seller.ToolService;

import java.time.LocalDate;
import java.util.Map;

@Controller
@RequestMapping("/seller")
@RequiredArgsConstructor
public class SellerReportController {

    private final ToolService toolService;
    private final SellerReportService reportService;

    // Trang hiển thị chính
    @GetMapping("/tools/sales-report")
    public String showSalesReport(@RequestParam(value = "toolId", required = false) Long toolId,
                                  HttpServletRequest request, Model model) {

        Account seller = (Account) request.getSession().getAttribute("loggedInAccount");
        if (seller == null) throw new RuntimeException("Bạn chưa đăng nhập.");

        model.addAttribute("toolName", "Tất cả tool");
        model.addAttribute("summary", reportService.getSummary(seller, toolId, null, null, "all"));
        model.addAttribute("feedback", reportService.getFeedbackSummary(seller, toolId));
        model.addAttribute("revenueChartData", reportService.getRevenueChart(seller, toolId, "month", null, null, "all"));
        model.addAttribute("methodChartData", reportService.getLoginMethodChart(seller, toolId, null, null, "all"));

        return "seller/sales-report";
    }

    // API dữ liệu filter động
    @GetMapping("/tools/sales-report/data")
    @ResponseBody
    public Map<String, Object> getSalesData(
            @RequestParam(value = "toolId", required = false) Long toolId,
            @RequestParam(value = "filter", defaultValue = "day") String filter,
            @RequestParam(value = "method", defaultValue = "all") String method,
            @RequestParam(value = "start", required = false) String startStr,
            @RequestParam(value = "end", required = false) String endStr,
            HttpServletRequest request) {

        Account seller = (Account) request.getSession().getAttribute("loggedInAccount");
        LocalDate start = (startStr == null || startStr.isEmpty()) ? null : LocalDate.parse(startStr);
        LocalDate end = (endStr == null || endStr.isEmpty()) ? null : LocalDate.parse(endStr);

        var revenue = reportService.getRevenueChart(seller, toolId, filter, start, end, method);
        var methodChart = reportService.getLoginMethodChart(seller, toolId, start, end, method);

        boolean isEmpty = (revenue.size() == 1 && revenue.get(0).containsKey("empty"))
                || (methodChart.size() == 1 && methodChart.get(0).containsKey("empty"));

        if (isEmpty) {
            return Map.of("empty", true);
        }

        return Map.of(
                "summary", reportService.getSummary(seller, toolId, start, end, method),
                "revenue", revenue,
                "method", methodChart
        );
    }

    // API feedback riêng
    @GetMapping("/tools/sales-report/feedback")
    @ResponseBody
    public Map<String, Object> getFeedbackData(
            @RequestParam(value = "toolId", required = false) Long toolId,
            HttpServletRequest request) {

        Account seller = (Account) request.getSession().getAttribute("loggedInAccount");
        return reportService.getFeedbackSummary(seller, toolId);
    }
}
