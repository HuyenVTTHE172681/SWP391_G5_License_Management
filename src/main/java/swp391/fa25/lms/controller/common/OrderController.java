package swp391.fa25.lms.controller.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.CustomerOrder;
import swp391.fa25.lms.model.WalletTransaction;
import swp391.fa25.lms.repository.CustomerOrderRepository;
import swp391.fa25.lms.repository.WalletRepository;
import swp391.fa25.lms.repository.WalletTransactionRepository;
import org.springframework.ui.Model;
import swp391.fa25.lms.service.customer.OrderService;

import java.util.List;

@Controller
public class OrderController {
    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private WalletTransactionRepository transactionRepository;

    @Autowired
    private OrderService orderService;

    /**
     * Trang hiển thị danh sách đơn hàng mà người dùng đã mua (full page)
     */
    @GetMapping("/orders")
    public String viewOrders(
            HttpSession session,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String dateRange,
            @RequestParam(required = false) String priceRange,
            @RequestParam(defaultValue = "createdAt") String sortField,
            @RequestParam(defaultValue = "desc") String sortDir,
            Model model) {
        // Lấy tài khoản đang đăng nhập từ session
        Account account = (Account) session.getAttribute("loggedInAccount");
        if (account == null) return "redirect:/login";

        // Gọi service xử lý logic lọc + phân trang
        Page<CustomerOrder> ordersPage = orderService.getFilteredOrders(
                account, keyword, status, dateRange, priceRange, page, size, sortField, sortDir
        );

        // Gán các attribute chung vào model để Thymeleaf render
        addCommonAttributes(model, ordersPage, page, keyword, status, dateRange, priceRange, sortField, sortDir, size);
        return "customer/orders";
    }

    /**
     * Trả về fragment HTML (phần bảng đơn hàng) -- dùng cho HTMX / AJAX. Endpoit /orders/filter
     * Khi người dùng thay đổi filter, sort, hoặc nhấn phân trang → HTMX sẽ gọi endpoint /orders/filter
     */
    @GetMapping("/orders/filter")
    public String filterOrdersFragment(
            HttpServletRequest request,
            HttpSession session,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String dateRange,
            @RequestParam(required = false) String priceRange,
            @RequestParam(defaultValue = "createdAt") String sortField,
            @RequestParam(defaultValue = "desc") String sortDir,
            Model model) {
        // Lấy tài khoản đang đăng nhập từ session
        Account account = (Account) session.getAttribute("loggedInAccount");
        if (account == null) return "redirect:/login";

        // Gọi service xử lý logic lọc + phân trang
        Page<CustomerOrder> ordersPage = orderService.getFilteredOrders(
                account, keyword, status, dateRange, priceRange, page, size, sortField, sortDir
        );

        // Gán các attribute chung vào model để Thymeleaf render
        addCommonAttributes(model, ordersPage, page, keyword, status, dateRange, priceRange, sortField, sortDir, size);

        // Nếu là HTMX request (AJAX), trả về fragment chứa bảng + pagination
        String hxRequest = request.getHeader("HX-Request");
        if (hxRequest != null && hxRequest.equalsIgnoreCase("true")) {
            // fragment name: customer/orders :: orderTableFragment
            return "customer/orders :: orderTableFragment";
        }

        return "customer/orders";
    }

    /**
     * Hàm dùng chung để thêm các attribute vào model
     * @param model
     * @param ordersPage: xử lý lọc + phân trang
     * @param page: trang hiện tại (bắt đầu từ 0)
     * @param keyword: từ khóa tìm kiếm
     * @param status: lọc theo trạng thái đơn hàng
     * @param dateRange: lọc theo thời gian
     * @param priceRange: lọc theo giá
     * @param sortField: cột sắp xếp
     * @param sortDir: hướng sắp xếp tăng, giảm
     * @param size: số item mỗi trang (5)
     */
    private void addCommonAttributes(Model model, Page<CustomerOrder> ordersPage,
                                     int page, String keyword, String status,
                                     String dateRange, String priceRange,
                                     String sortField, String sortDir, int size) {
        model.addAttribute("ordersPage", ordersPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", ordersPage.getTotalPages());
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("dateRange", dateRange);
        model.addAttribute("priceRange", priceRange);
        model.addAttribute("sortField", sortField);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("size", size);
    }


    /**
     * Trang hiển thị lịch sử giao dịch thanh toán (Wallet_Transaction)
     */
    @GetMapping("/payment-history")
    public String viewPaymentHistory(HttpSession session, Model model) {
        Account account = (Account) session.getAttribute("loggedInAccount");
        if (account == null) {
            return "redirect:/login";
        }

        // Lấy ví của user (buyer hoặc seller đều có thể xem)
        var walletOpt = walletRepository.findByAccount(account);
        if (walletOpt.isEmpty()) {
            model.addAttribute("transactions", List.of());
            return "customer/payment-history";
        }

        var wallet = walletOpt.get();
        List<WalletTransaction> transactions = transactionRepository.findByWalletOrderByCreatedAtDesc(wallet);

        model.addAttribute("transactions", transactions);
        model.addAttribute("wallet", wallet);
        return "customer/payment-history"; // template payment-history.html
    }
}
