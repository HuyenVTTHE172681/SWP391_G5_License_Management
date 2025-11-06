package swp391.fa25.lms.controller.seller;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.CustomerOrder;
import swp391.fa25.lms.service.seller.SellerOrderService;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/seller/orders")
public class SellerOrderController {

    @Autowired
    private SellerOrderService sellerOrderService;

    @GetMapping
    public String viewOrders(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false, defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "6") int size,
            Model model,
            HttpSession session
    ) {
        Account seller = (Account) session.getAttribute("loggedInAccount");

        // 👉 Lọc dữ liệu
        List<CustomerOrder> filteredOrders = sellerOrderService.getFilteredOrders(seller, keyword, status, from, to, sort);

        // 💰 Tổng doanh thu
        double totalRevenue = sellerOrderService.calculateTotalRevenue(filteredOrders);

        // 📄 Phân trang
        int totalOrders = filteredOrders.size();
        int totalPages = (int) Math.ceil((double) totalOrders / size);
        List<CustomerOrder> pageOrders = sellerOrderService.paginate(filteredOrders, page, size);

        // 🧾 Gửi dữ liệu ra view
        model.addAttribute("orders", pageOrders);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("sort", sort);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);

        return "seller/orders-list";
    }
}
