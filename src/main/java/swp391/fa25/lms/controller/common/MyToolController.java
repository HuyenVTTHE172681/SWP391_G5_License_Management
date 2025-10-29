package swp391.fa25.lms.controller.common;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import swp391.fa25.lms.model.*;
import swp391.fa25.lms.repository.CustomerOrderRepository;
import swp391.fa25.lms.repository.LicenseAccountRepository;
import swp391.fa25.lms.repository.LicenseRenewLogRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Controller
@RequestMapping("/my-tools")
public class MyToolController {

    private final CustomerOrderRepository orderRepo;
    private final LicenseAccountRepository accRepo;
    private final LicenseRenewLogRepository renewRepo;

    public MyToolController(CustomerOrderRepository orderRepo,
                            LicenseAccountRepository accRepo,
                            LicenseRenewLogRepository renewRepo) {
        this.orderRepo = orderRepo;
        this.accRepo = accRepo;
        this.renewRepo = renewRepo;
    }

    private CustomerOrder loadOrderOr404(Long orderId) {
        return orderRepo.findByOrderId(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng"));
    }

    @GetMapping("/{orderId}")
    @Transactional(readOnly = true)
    public String viewTool(@PathVariable Long orderId, Model model) {
        var order = loadOrderOr404(orderId);
        if (order.getOrderStatus() != CustomerOrder.OrderStatus.SUCCESS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn hàng chưa thành công.");
        }
        var acc = order.getLicenseAccount();
        if (acc == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn hàng chưa được cấp license.");

        model.addAttribute("order", order);
        model.addAttribute("tool", order.getTool());
        model.addAttribute("acc", acc);

        var method = acc.getLoginMethod() != null
                ? acc.getLoginMethod()
                : (order.getTool().getLoginMethod() == Tool.LoginMethod.TOKEN
                ? LicenseAccount.LoginMethod.TOKEN
                : LicenseAccount.LoginMethod.USER_PASSWORD);

        return (method == LicenseAccount.LoginMethod.TOKEN)
                ? "customer/mytool-token"
                : "customer/mytool-userpass";
    }

    // ===== Gia hạn + ghi LicenseRenewLog =====
    @PostMapping("/{orderId}/renew")
    @Transactional
    public String renew(@PathVariable Long orderId, RedirectAttributes ra) {
        var order = loadOrderOr404(orderId);
        if (order.getOrderStatus() != CustomerOrder.OrderStatus.SUCCESS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn chưa thành công.");
        }
        var acc = order.getLicenseAccount();
        if (acc == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn chưa được cấp license.");
        int days = (order.getLicense() != null && order.getLicense().getDurationDays() != null)
                ? order.getLicense().getDurationDays()
                : 30;
        var now = LocalDateTime.now();
        var base = (acc.getEndDate() != null && acc.getEndDate().isAfter(now)) ? acc.getEndDate() : now;
        var newEnd = base.plusDays(days);
        acc.setEndDate(newEnd);
        if (acc.getStartDate() == null) acc.setStartDate(now);
        acc.setStatus(LicenseAccount.Status.ACTIVE);
        accRepo.save(acc);
        var log = new LicenseRenewLog();
        log.setLicenseAccount(acc);
        log.setRenewDate(now);
        log.setNewEndDate(newEnd);
        BigDecimal amount = BigDecimal.ZERO;
        if (order.getLicense() != null && order.getLicense().getPrice() != null) {
            amount = BigDecimal.valueOf(order.getLicense().getPrice());
        } else if (order.getPrice() != null) {
            amount = BigDecimal.valueOf(order.getPrice());
        }
        log.setAmountPaid(amount);
        renewRepo.save(log);

        ra.addFlashAttribute("ok", "Đã gia hạn thêm " + days + " ngày. Hạn mới: " + newEnd);
        return "redirect:/my-tools/" + orderId;
    }

    // ===== Update user/pass như trước (chỉ cho USER_PASSWORD) =====
    @PostMapping("/{orderId}/account")
    @Transactional
    public String updateAccount(@PathVariable Long orderId,
                                @RequestParam String username,
                                @RequestParam String password,
                                RedirectAttributes ra) {
        var order = loadOrderOr404(orderId);
        var acc = order.getLicenseAccount();
        if (acc == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn chưa được cấp license.");
        if (acc.getLoginMethod() == LicenseAccount.LoginMethod.TOKEN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tài khoản dùng TOKEN: không thể đổi username/password.");
        }
        acc.setUsername(username.trim());
        acc.setPassword(password); // Nếu cần encode: BCrypt
        accRepo.save(acc);
        ra.addFlashAttribute("ok", "Đã cập nhật tài khoản đăng nhập.");
        return "redirect:/my-tools/" + orderId;
    }
}
