package swp391.fa25.lms.controller.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.CustomerOrder;
import swp391.fa25.lms.model.Tool;
import swp391.fa25.lms.repository.OrderRepository;
import swp391.fa25.lms.service.customer.PaymentPayPalService;
import swp391.fa25.lms.service.customer.ToolService;
import java.util.Optional;

@Controller
@RequestMapping("/paypal-payment")
public class PaymentPayPalController {
    @Autowired
    private PaymentPayPalService payPalService;
    @Autowired
    private ToolService toolService;
    @Autowired
    private OrderRepository orderRepository;

    /**
     * Tạo thanh toán PayPal — khi click “Thanh toán” paypal-payment/create
     * Tạo PENDING order → Redirect PayPal (hỗ trợ retry nếu orderId)
     */
    @GetMapping("/create")
    public RedirectView createPayPalPayment(@RequestParam Long toolId,
                                            @RequestParam Long licenseId,
                                            @RequestParam(required = false) Long orderId,
                                            HttpServletRequest request,
                                            Model model) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("loggedInAccount") == null) {
            return new RedirectView("/login");
        }
        Account account = (Account) session.getAttribute("loggedInAccount");
        Tool tool = toolService.findById(toolId)
                .orElseThrow(() -> new RuntimeException("Tool không tồn tại"));
        if (tool.getQuantity() <= 0) {
            model.addAttribute("errorMessage", "Sản phẩm này đã hết hàng!");
            return new RedirectView("/tools/" + toolId);
        }

        // THÊM: Nếu có orderId (retry), check PENDING (tương tự VNPay)
        if (orderId != null) {
            Optional<CustomerOrder> optionalOrder = orderRepository.findById(orderId);
            if (optionalOrder.isPresent() && optionalOrder.get().getOrderStatus() == CustomerOrder.OrderStatus.PENDING) {
                // Retry order PENDING
                String paymentUrl = payPalService.createPaymentForRetry(orderId, licenseId, account, request);
                return new RedirectView(paymentUrl);
            } else {
                model.addAttribute("errorMessage", "Đơn hàng không hợp lệ hoặc đã thanh toán!");
                return new RedirectView("/orders");
            }
        }

        String redirectUrl = payPalService.createPayment(toolId, licenseId, account, request);
        return new RedirectView(redirectUrl);
    }

    @GetMapping("/return")
    public RedirectView returnFromPayPal(@RequestParam("paymentId") String paymentId, @RequestParam("PayerID") String payerId, @RequestParam("orderId") Long orderId) {
        boolean success = payPalService.executePayment(paymentId, payerId, orderId);
        return new RedirectView(success ? "/paypal-payment/payment-success?orderId=" + orderId : "/paypal-payment/payment-fail?orderId=" + orderId);
    }

    @GetMapping("/cancel")
    public RedirectView cancelPayment(@RequestParam("orderId") Long orderId) {
        payPalService.cancelPayment(orderId);  // Update status CANCELLED
        return new RedirectView("/paypal-payment/payment-cancel?orderId=" + orderId);
    }

    //  paymentReturn
    @GetMapping("/payment-success")
    public String paymentSuccess(@RequestParam Long orderId, Model model) {
        Optional<CustomerOrder> optionalOrder = orderRepository.findById(orderId);
        if (optionalOrder.isPresent()) {
            CustomerOrder order = optionalOrder.get();
            model.addAttribute("order", order);
            model.addAttribute("success", true);  // Để Thymeleaf phân biệt
            model.addAttribute("message", "Thanh toán thành công! Email xác nhận đã gửi.");
        } else {
            model.addAttribute("error", "Không tìm thấy đơn hàng.");
        }
        return "public/payment-success";  // View mới
    }

    @GetMapping("/payment-fail")
    public String paymentFail(@RequestParam Long orderId, Model model) {
        Optional<CustomerOrder> optionalOrder = orderRepository.findById(orderId);
        if (optionalOrder.isPresent()) {
            CustomerOrder order = optionalOrder.get();
            model.addAttribute("order", order);
            model.addAttribute("success", false);
            model.addAttribute("message", "Thanh toán thất bại. Vui lòng thử lại.");
        } else {
            model.addAttribute("error", "Không tìm thấy đơn hàng.");
        }
        return "public/payment-fail";  // View mới
    }

    @GetMapping("/payment-cancel")
    public String paymentCancel(@RequestParam Long orderId, Model model) {
        Optional<CustomerOrder> optionalOrder = orderRepository.findById(orderId);
        if (optionalOrder.isPresent()) {
            CustomerOrder order = optionalOrder.get();
            model.addAttribute("order", order);
            model.addAttribute("success", false);
            model.addAttribute("message", "Bạn đã hủy thanh toán. Có thể thử lại.");
        } else {
            model.addAttribute("error", "Không tìm thấy đơn hàng.");
        }
        return "public/payment-cancel";  // View mới
    }
}