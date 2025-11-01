package swp391.fa25.lms.controller.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.License;
import swp391.fa25.lms.service.customer.MyToolService;
import swp391.fa25.lms.service.customer.PaymentRenewService;
import swp391.fa25.lms.service.customer.PaymentService;

import java.util.Map;

@Controller
@RequestMapping({"/renew-payment", "/renew-pay"})
public class RenewPaymentController {

    private final MyToolService myToolService;
    private final PaymentRenewService paymentService; // ✅ inject cho VNPay renew

    public RenewPaymentController(@Qualifier("myTool") MyToolService myToolService,
                                  PaymentRenewService paymentService) {
        this.myToolService = myToolService;
        this.paymentService = paymentService;
    }

    /** Trang sandbox chọn gói gia hạn (xem thông tin trước khi thanh toán) */
    @GetMapping
    public String sandbox(@RequestParam Long orderId,
                          @RequestParam Long licenseId,
                          HttpServletRequest request,
                          Model model,
                          RedirectAttributes ra) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("loggedInAccount") == null) {
            return "redirect:/login";
        }
        Account login = (Account) session.getAttribute("loggedInAccount");

        try {
            var vd = myToolService.viewTool(orderId);
            if (vd.order() == null || vd.order().getAccount() == null ||
                    !vd.order().getAccount().getAccountId().equals(login.getAccountId())) {
                ra.addFlashAttribute("err", "Bạn không có quyền truy cập đơn hàng này.");
                return "redirect:/orders";
            }

            License lic = vd.licenses().stream()
                    .filter(l -> l.getLicenseId().equals(licenseId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Gói license không hợp lệ."));

            model.addAttribute("order", vd.order());
            model.addAttribute("tool", vd.tool());
            model.addAttribute("acc", vd.acc());
            model.addAttribute("license", lic);
            model.addAttribute("orderId", orderId);
            model.addAttribute("licenseId", licenseId);
            return "public/payment-sandbox";
        } catch (Exception ex) {
            ra.addFlashAttribute("err", "Không thể tải thông tin gia hạn: " + ex.getMessage());
            return "redirect:/orders";
        }
    }

    /** Người dùng bấm 'Thanh toán VNPay' cho gia hạn -> tạo URL và redirect sang VNPay */
    @PostMapping("/vnpay")
    public String createRenewVnpay(@RequestParam Long orderId,
                                   @RequestParam Long licenseId,
                                   HttpServletRequest request,
                                   RedirectAttributes ra) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("loggedInAccount") == null) {
            return "redirect:/login";
        }
        Account login = (Account) session.getAttribute("loggedInAccount");

        try {
            var vd = myToolService.viewTool(orderId);
            if (vd.order() == null || vd.order().getAccount() == null ||
                    !vd.order().getAccount().getAccountId().equals(login.getAccountId())) {
                ra.addFlashAttribute("err", "Bạn không có quyền truy cập đơn hàng này.");
                return "redirect:/orders";
            }

            String url = paymentService.createRenewPaymentUrl(orderId, licenseId, login, request);
            return "redirect:" + url;
        } catch (Exception ex) {
            ra.addFlashAttribute("err", "Không thể tạo thanh toán VNPay: " + ex.getMessage());
            return "redirect:/renew-payment?orderId=" + orderId + "&licenseId=" + licenseId;
        }
    }

    /** VNPay gọi về sau gia hạn (returnUrlRenew) */
    @GetMapping("/vnpay-return")
    public String vnpayReturn(@RequestParam Map<String, String> params) {
        boolean ok = paymentService.handleRenewCallback(params);
        return "redirect:/renew-payment/result?success=" + ok;
    }

    /** Trang thông báo kết quả */
    @GetMapping("/result")
    public String result(@RequestParam(defaultValue = "false") boolean success, Model model) {
        model.addAttribute("success", success);
        return "public/renewPayment-result";
    }
}
