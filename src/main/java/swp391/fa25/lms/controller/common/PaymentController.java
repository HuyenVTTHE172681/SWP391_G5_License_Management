package swp391.fa25.lms.controller.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.service.customer.PaymentService;

import java.util.Map;

@Controller
@RequestMapping("/payment")
public class PaymentController {
    @Autowired
    private PaymentService paymentService;

    /**
     * Create payment: redirect user to VNPay payment URL.
     * We MUST get buyer Account from HttpSession (your app stores loggedInAccount there).
     */
    @GetMapping("/create")
    public RedirectView createPayment(@RequestParam Long toolId,
                                      @RequestParam Long licenseId,
                                      HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            // No session → force login
            return new RedirectView("/login");
        }

        Account account = (Account) session.getAttribute("loggedInAccount");
        if (account == null) {
            // Not logged in according to session → redirect login
            return new RedirectView("/login");
        }

        // Create VNPay url via service (this returns full url to redirect)
        String paymentUrl = paymentService.createPaymentUrl(toolId, licenseId, account, request);

        // Redirect client to VNPay (sandbox)
        return new RedirectView(paymentUrl);
    }

    /**
     * VNPay return URL (user redirected here after paying)
     * VNPay sends many query params; Spring can collect them into Map.
     */
    @GetMapping("/vnpay-return")
    public String paymentReturn(@RequestParam Map<String, String> params,
                                Map<String, Object> model) {
        boolean success = paymentService.handlePaymentCallback(params);
        String orderInfo = params.get("vnp_OrderInfo");

        // ⚡ Phân biệt loại giao dịch
        if (orderInfo != null && orderInfo.startsWith("SELLER_")) {
            if (success) {
                return "seller/paymentSuccess";
            } else {
                return "seller/paymentFailed";
            }
        }

        // 🧾 Thanh toán tool
        model.put("success", success);
        model.put("vnpParams", params);
        return "public/payment-result";
    }
}
