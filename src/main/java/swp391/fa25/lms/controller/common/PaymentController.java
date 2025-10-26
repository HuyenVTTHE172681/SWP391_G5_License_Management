package swp391.fa25.lms.controller.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.Tool;
import swp391.fa25.lms.service.customer.PaymentService;
import swp391.fa25.lms.service.customer.ToolService;

import java.util.Map;

@Controller
@RequestMapping("/payment")
public class PaymentController {
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private ToolService toolService;

    /**
     * Tạo thanh toán — khi click “Thanh toán” payment/create
     */
    @GetMapping("/create")
    public RedirectView createPayment(@RequestParam Long toolId,
                                      @RequestParam Long licenseId,
                                      HttpServletRequest request,
                                      Model model) {
        // Kiểm tra session (nếu chưa đăng nhập thì bắt đăng nhập lại)
        HttpSession session = request.getSession(false);
        if (session == null) {
            return new RedirectView("/login");
        }

        Tool tool = toolService.findById(toolId)
                .orElseThrow(() -> new RuntimeException("Tool không tồn tại"));

        if (tool.getQuantity() <= 0) {
            model.addAttribute("errorMessage", "Sản phẩm này đã hết hàng!");
        }

        // Lấy thông tin account đăng nhập từ session
        Account account = (Account) session.getAttribute("loggedInAccount");
        if (account == null) {
            // Not logged in according to session → redirect login
            return new RedirectView("/login");
        }

        // Gọi service để tạo URL thanh toán VNPay
        String paymentUrl = paymentService.createPaymentUrl(toolId, licenseId, account, request);

        // Redirect sang VNPay để thực hiện thanh toán
        return new RedirectView(paymentUrl);
    }

    /**
     * Callback sau khi thanh toán xong — VNPay sẽ redirect payment/vnpay-return
     */
    @GetMapping("/vnpay-return")
    public String paymentReturn(@RequestParam Map<String, String> params,
                                Map<String, Object> model) {
        // Gọi service xử lý callback từ VNPay
        boolean success = paymentService.handlePaymentCallback(params);

        // Kết quả ra view
        model.put("success", success);
        model.put("vnpParams", params);

        return "public/payment-result"; // Trả về kết quả thanh toán
    }
}
