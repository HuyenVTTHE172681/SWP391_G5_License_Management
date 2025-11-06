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

@Controller
@RequestMapping("/seller")
@RequiredArgsConstructor
public class SellerReportController {

    private final SellerReportService reportService;

    @GetMapping("/tools/sales-report")
    public String showSalesReport(
            @RequestParam(value = "toolId", required = false) Long toolId,
            @RequestParam(value = "filter", defaultValue = "month") String filter,
            @RequestParam(value = "method", defaultValue = "all") String method,
            @RequestParam(value = "start", required = false) String startStr,
            @RequestParam(value = "end", required = false) String endStr,
            HttpServletRequest request,
            Model model) {

        Account seller = (Account) request.getSession().getAttribute("loggedInAccount");
        if (seller == null) throw new RuntimeException("Bạn chưa đăng nhập.");

        LocalDate start = (startStr == null || startStr.isEmpty()) ? null : LocalDate.parse(startStr);
        LocalDate end = (endStr == null || endStr.isEmpty()) ? null : LocalDate.parse(endStr);

        model.addAttribute("toolName", "Tất cả tool");
        model.addAttribute("summary", reportService.getSummary(seller, toolId, start, end, method));
        model.addAttribute("feedback", reportService.getFeedbackSummary(seller, toolId));
        model.addAttribute("revenueChartData", reportService.getRevenueChart(seller, toolId, filter, start, end, method));
        model.addAttribute("methodChartData", reportService.getLoginMethodChart(seller, toolId, start, end, method));
        model.addAttribute("filter", filter);
        model.addAttribute("method", method);

        return "seller/sales-report";
    }
}
