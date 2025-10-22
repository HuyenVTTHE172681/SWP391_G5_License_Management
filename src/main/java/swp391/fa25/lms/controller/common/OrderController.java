package swp391.fa25.lms.controller.common;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.CustomerOrder;
import swp391.fa25.lms.model.WalletTransaction;
import swp391.fa25.lms.repository.CustomerOrderRepository;
import swp391.fa25.lms.repository.WalletRepository;
import swp391.fa25.lms.repository.WalletTransactionRepository;
import org.springframework.ui.Model;

import java.util.List;

@Controller
public class OrderController {
    @Autowired
    private CustomerOrderRepository orderRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private WalletTransactionRepository transactionRepository;

    /**
     * Trang hiển thị danh sách đơn hàng mà người dùng đã mua
     */
    @GetMapping("/orders")
    public String viewOrders(HttpSession session, Model model) {
        // Lấy thông tin người dùng đang đăng nhập
        Account account = (Account) session.getAttribute("loggedInAccount");
        if (account == null) {
            return "redirect:/login"; // nếu chưa login → chuyển đến trang login
        }

        // Lấy danh sách các đơn hàng của user
        List<CustomerOrder> orders = orderRepository.findByAccount_AccountIdOrderByCreatedAtDesc(account.getAccountId());

        // Đưa danh sách order sang view
        model.addAttribute("orders", orders);
        return "customer/orders"; // Trả về template orders.html
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
