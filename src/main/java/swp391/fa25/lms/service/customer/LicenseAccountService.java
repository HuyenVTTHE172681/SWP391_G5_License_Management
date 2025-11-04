package swp391.fa25.lms.service.customer;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import swp391.fa25.lms.model.*;
import swp391.fa25.lms.repository.CustomerOrderRepository;
import swp391.fa25.lms.repository.LicenseAccountRepository;
import swp391.fa25.lms.repository.LicenseToolRepository;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Xử lý: tạo license account sau thanh toán, xem/điều chỉnh password/token,
 * mark activated/revoke, và build view model cho trang my-license.
 */
@Service
public class LicenseAccountService {

    private final CustomerOrderRepository orderRepo;
    private final LicenseAccountRepository accRepo;
    private final LicenseToolRepository licenseRepo;

    public LicenseAccountService(CustomerOrderRepository orderRepo,
                                 LicenseAccountRepository accRepo,
                                 LicenseToolRepository licenseRepo) {
        this.orderRepo = orderRepo;
        this.accRepo = accRepo;
        this.licenseRepo = licenseRepo;
    }

    /** ViewModel trả về cho trang Thymeleaf */
    public record LicenseVM(CustomerOrder order, Tool tool, License license, LicenseAccount account) {}

    /** Tải view model; nếu created==true thì cố gắng auto-create nếu chưa có. */
    @Transactional
    public LicenseVM getOrCreateIfPaid(Long orderId, String currentEmail, boolean createIfMissing) {
        CustomerOrder order = findOwnedOrder(orderId, currentEmail);

        // (Tuỳ dự án) chỉ cho phép vào khi đơn hàng đã thanh toán thành công
        if (!isOrderPaid(order)) {
            throw new IllegalArgumentException("Order is not paid yet.");
        }

        LicenseAccount acc = accRepo.findByOrder_OrderId(orderId).orElse(null);
        if (acc == null && createIfMissing) {
            acc = createAccountForOrder(order);
        }
        License license = order.getLicense(); // giả định có quan hệ
        Tool tool = order.getTool();          // giả định có quan hệ; nếu không, lấy từ license.getTool()

        if (tool == null && license != null) tool = license.getTool();
        return new LicenseVM(order, tool, license, acc);
    }

    /** Tạo nếu chưa có (nút Create trên UI) */
    @Transactional
    public LicenseAccount createForOrderIfAbsent(Long orderId, String currentEmail) {
        CustomerOrder order = findOwnedOrder(orderId, currentEmail);
        if (!isOrderPaid(order)) throw new IllegalArgumentException("Order is not paid yet.");

        return accRepo.findByOrder_OrderId(orderId).orElseGet(() -> createAccountForOrder(order));
    }

    /** Xoay mật khẩu (USER_PASSWORD) */
    @Transactional
    public String rotatePassword(Long orderId, String currentEmail) {
        LicenseAccount acc = getOwnedAccountByOrder(orderId, currentEmail);
        if (acc.getLoginMethod() != LicenseAccount.LoginMethod.USER_PASSWORD)
            throw new IllegalArgumentException("This license uses TOKEN login; cannot rotate password.");

        String newPwd = randomPassword();
        acc.setPassword(newPwd);
        accRepo.save(acc);
        return newPwd;
    }

    /** Sinh token mới (TOKEN) */
    @Transactional
    public String regenerateToken(Long orderId, String currentEmail) {
        LicenseAccount acc = getOwnedAccountByOrder(orderId, currentEmail);
        if (acc.getLoginMethod() != LicenseAccount.LoginMethod.TOKEN)
            throw new IllegalArgumentException("This license uses USER_PASSWORD; cannot regenerate token.");

        String newToken = randomToken();
        acc.setToken(newToken);
        acc.setUsed(false);
        accRepo.save(acc);
        return newToken;
    }

    /** Đánh dấu đã kích hoạt */
    @Transactional
    public void markActivated(Long orderId, String currentEmail) {
        LicenseAccount acc = getOwnedAccountByOrder(orderId, currentEmail);
        acc.setUsed(true);
        if (acc.getStatus() == null) acc.setStatus(LicenseAccount.Status.ACTIVE);
        accRepo.save(acc);
    }

    /** Thu hồi */
    @Transactional
    public void revoke(Long orderId, String currentEmail) {
        LicenseAccount acc = getOwnedAccountByOrder(orderId, currentEmail);
        acc.setStatus(LicenseAccount.Status.REVOKED);
        accRepo.save(acc);
    }

    // ====================== Helpers ======================

    private LicenseAccount createAccountForOrder(CustomerOrder order) {
        Tool tool = order.getTool() != null ? order.getTool()
                : (order.getLicense() != null ? order.getLicense().getTool() : null);
        if (tool == null) throw new IllegalArgumentException("Tool not found for this order.");

        LicenseAccount acc = new LicenseAccount();
        acc.setOrder(order);
        acc.setTool(tool);
        acc.setLicense(order.getLicense());
        acc.setStatus(LicenseAccount.Status.ACTIVE);
        acc.setStartDate(LocalDateTime.now());

        Integer duration = order.getLicense() != null ? order.getLicense().getDurationDays() : null;
        if (duration != null && duration > 0) {
            acc.setEndDate(LocalDateTime.now().plusDays(duration));
        }

        // chọn theo login method của Tool
        LicenseAccount.LoginMethod lm = tool.getLoginMethod() == Tool.LoginMethod.TOKEN
                ? LicenseAccount.LoginMethod.TOKEN
                : LicenseAccount.LoginMethod.USER_PASSWORD;
        acc.setLoginMethod(lm);

        if (lm == LicenseAccount.LoginMethod.USER_PASSWORD) {
            // username gợi ý: email buyer hoặc "user-{orderId}"
            String suggestedUser = safeDefaultUsername(order);
            acc.setUsername(suggestedUser);
            acc.setPassword(randomPassword());
        } else {
            acc.setToken(randomToken());
            acc.setUsed(false);
        }

        return accRepo.save(acc);
    }

    private String randomPassword() {
        // 12 ký tự chữ + số
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private String randomToken() {
        // 24 ký tự chữ + số + "_" theo regex trong model
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12)
                + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private String safeDefaultUsername(CustomerOrder order) {
        try {
            String email = order.getAccount().getEmail(); // <-- đổi getter cho đúng với model của bạn
            if (StringUtils.hasText(email)) {
                int at = email.indexOf('@');
                return (at > 0 ? email.substring(0, at) : email) + "_" + order.getOrderId();
            }
        } catch (Exception ignored) {}
        return "user_" + order.getOrderId();
    }

    private LicenseAccount getOwnedAccountByOrder(Long orderId, String currentEmail) {
        CustomerOrder order = findOwnedOrder(orderId, currentEmail);
        return accRepo.findByOrder_OrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("No license account found for this order."));
    }

    /** Đảm bảo đơn hàng thuộc về currentEmail */
    private CustomerOrder findOwnedOrder(Long orderId, String currentEmail) {
        var opt = orderRepo.findById(orderId);
        if (opt.isEmpty()) throw new IllegalArgumentException("Order not found.");

        CustomerOrder order = opt.get();
        // TODO: kiểm tra chủ sở hữu tuỳ field của bạn:
        // ví dụ nếu CustomerOrder có getCustomer().getEmail()
        try {
            String ownerEmail = order.getAccount().getEmail();
            if (!currentEmail.equalsIgnoreCase(ownerEmail)) {
                throw new IllegalArgumentException("You don't have permission for this order.");
            }
        } catch (Exception e) {
            // Nếu field khác tên, sửa lại cho đúng.
            // throw mới để tránh bypass
            throw new IllegalArgumentException("Ownership validation failed. Please adjust field mapping.");
        }
        return order;
    }

    private boolean isOrderPaid(CustomerOrder order) {
        // TODO: thay bằng logic thực (VD: order.getStatus() == PAID/COMPLETED)
        try {
            String st = order.getOrderStatus().name();
            return "PAID".equalsIgnoreCase(st) || "COMPLETED".equalsIgnoreCase(st) || "SUCCESS".equalsIgnoreCase(st);
        } catch (Exception e) {
            return true; // tạm cho vào nếu bạn chưa làm trạng thái
        }
    }
}
