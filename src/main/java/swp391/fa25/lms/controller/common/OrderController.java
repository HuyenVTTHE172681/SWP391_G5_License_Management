package swp391.fa25.lms.controller.common;

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
     * Trang hiển thị danh sách đơn hàng mà người dùng đã mua
     */
    @GetMapping("/orders")
    public String viewOrders(
            HttpSession session,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "createdAt") String sortField,
            @RequestParam(defaultValue = "desc") String sortDir,
            Model model) {

        Account account = (Account) session.getAttribute("loggedInAccount");
        if (account == null) return "redirect:/login";

        Page<CustomerOrder> ordersPage = orderService.getFilteredOrders(
                account, keyword, status, page, size, sortField, sortDir
        );

        model.addAttribute("ordersPage", ordersPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", ordersPage.getTotalPages());
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("sortField", sortField);
        model.addAttribute("sortDir", sortDir);

        return "customer/orders";
    }

    /**
     * Trả về fragment HTML (phần bảng đơn hàng)
     */
    @GetMapping("/orders/filter")
    public String filterOrdersFragment(
            HttpSession session,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "createdAt") String sortField,
            @RequestParam(defaultValue = "desc") String sortDir,
            Model model) {

        Account account = (Account) session.getAttribute("loggedInAccount");
        if (account == null) return "redirect:/login";

        Page<CustomerOrder> ordersPage = orderService.getFilteredOrders(
                account, keyword, status, page, size, sortField, sortDir
        );

        model.addAttribute("ordersPage", ordersPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", ordersPage.getTotalPages());
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("sortField", sortField);
        model.addAttribute("sortDir", sortDir);

        // Trả về fragment (phần tbody + pagination)
        return "customer/order-table :: orderTable";
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
