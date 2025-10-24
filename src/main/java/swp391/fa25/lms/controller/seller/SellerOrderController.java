package swp391.fa25.lms.controller.seller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.CustomerOrder;
import swp391.fa25.lms.repository.CustomerOrderRepository;

import java.security.Principal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Controller
@RequestMapping("/seller/orders")
public class SellerOrderController {

    @Autowired
    private CustomerOrderRepository orderRepo;

    /**
     * Hiển thị & lọc danh sách đơn hàng của seller
     */
    @GetMapping
    public String viewOrders(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false, defaultValue = "newest") String sort,
            Model model,
            Principal principal
    ) {
        Account seller = getCurrentSeller(principal);
        List<CustomerOrder> orders = orderRepo.findByToolSeller(seller);

        // Lọc theo keyword
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.toLowerCase();
            orders = orders.stream()
                    .filter(o -> o.getTool().getToolName().toLowerCase().contains(kw)
                            || o.getAccount().getFullName().toLowerCase().contains(kw))
                    .toList();
        }

        // Lọc theo trạng thái
        if (status != null && !status.isBlank()) {
            orders = orders.stream()
                    .filter(o -> o.getOrderStatus().name().equalsIgnoreCase(status))
                    .toList();
        }

        // Lọc theo ngày tạo
        if (from != null) {
            orders = orders.stream()
                    .filter(o -> !o.getCreatedAt().toLocalDate().isBefore(from))
                    .toList();
        }
        if (to != null) {
            orders = orders.stream()
                    .filter(o -> !o.getCreatedAt().toLocalDate().isAfter(to))
                    .toList();
        }

        // Sắp xếp
        Comparator<CustomerOrder> cmp = switch (sort) {
            case "oldest" -> Comparator.comparing(CustomerOrder::getCreatedAt);
            case "highest" -> Comparator.comparing(CustomerOrder::getPrice).reversed();
            case "lowest" -> Comparator.comparing(CustomerOrder::getPrice);
            default -> Comparator.comparing(CustomerOrder::getCreatedAt).reversed(); // newest
        };
        orders = orders.stream().sorted(cmp).toList();

        // Tổng doanh thu
        double totalRevenue = orders.stream()
                .filter(o -> o.getOrderStatus() == CustomerOrder.OrderStatus.SUCCESS)
                .mapToDouble(CustomerOrder::getPrice)
                .sum();

        // Gửi dữ liệu ra view
        model.addAttribute("orders", orders);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("sort", sort);

        return "seller/orders-list";
    }

    /**
     * 🔧 Tạm thời giả lập seller hiện tại (sẽ thay bằng authentication sau)
     */
    private Account getCurrentSeller(Principal principal) {
        Account acc = new Account();
        acc.setAccountId(1L);
        return acc;
    }
}
