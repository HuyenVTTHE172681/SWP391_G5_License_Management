package swp391.fa25.lms.service.customer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import swp391.fa25.lms.model.*;
import swp391.fa25.lms.repository.*;

import java.math.BigDecimal;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

@Service("myTool")
public class MyToolService {

    private static final Pattern USERNAME_PATTERN =
            Pattern.compile("^[A-Za-z0-9._-]{3,100}$");
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)\\S{8,}$");

    @Autowired
    private CustomerOrderRepository orderRepo;
    @Autowired
    private LicenseAccountRepository accRepo;
    @Autowired
    private LicenseRenewLogRepository renewRepo;
    @Autowired
    private ToolFileRepository fileRepo;
    @Autowired
    private LicenseToolRepository licenseRepo;

    private final Path storageRoot = Paths.get("uploads");

    public MyToolService(CustomerOrderRepository orderRepo,
                         LicenseAccountRepository accRepo,
                         LicenseRenewLogRepository renewRepo,
                         ToolFileRepository fileRepo,
                         LicenseToolRepository licenseRepo) {
        this.orderRepo = orderRepo;
        this.accRepo = accRepo;
        this.renewRepo = renewRepo;
        this.fileRepo = fileRepo;
        this.licenseRepo = licenseRepo;
    }

    // ====================== PUBLIC API ======================

    /** Dữ liệu trang chi tiết tool theo order */
    @Transactional(readOnly = true)
    public ViewData viewTool(Long orderId) {
        CustomerOrder order = loadOrderOr404(orderId);
        ensureOrderSuccess(order);

        LicenseAccount acc = order.getLicenseAccount();
        if (acc == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn hàng chưa được cấp license.");
        }

        Tool tool = order.getTool();

        // Lấy loginMethod, nếu null thì default USER_PASSWORD
        Tool.LoginMethod method = tool.getLoginMethod() != null
                ? tool.getLoginMethod()
                : Tool.LoginMethod.USER_PASSWORD;
        tool.setLoginMethod(method); // để view dùng

        // 🔥 files:
        // - Nếu TOOL dùng TOKEN: chỉ lấy 1 file WRAPPED mới nhất
        // - Nếu USER_PASSWORD: giữ nguyên list cũ
        List<ToolFile> files;
        if (method == Tool.LoginMethod.TOKEN) {
            ToolFile latestWrapped = fileRepo
                    .findTopByTool_ToolIdAndFileTypeOrderByCreatedAtDesc(
                            tool.getToolId(),
                            ToolFile.FileType.WRAPPED
                    )
                    .orElse(null);

            files = (latestWrapped != null)
                    ? List.of(latestWrapped)
                    : List.of();
        } else {
            // trang user/pass vẫn cần list đầy đủ, để view tự hiển thị
            files = fileRepo.findByTool_ToolIdOrderByCreatedAtDesc(tool.getToolId());
        }

        List<License> licenses = licenseRepo.findByTool_ToolId(tool.getToolId());

        String template = (method == Tool.LoginMethod.TOKEN)
                ? "customer/mytool-token"
                : "customer/mytool-userpass";

        return new ViewData(order, tool, acc, files, licenses, template);
    }

    /** Tải file tool theo order */
    @Transactional(readOnly = true)
    public FileDownload download(Long orderId, Long fileId) {
        CustomerOrder order = loadOrderOr404(orderId);

        ToolFile f = fileRepo.findById(fileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File không tồn tại"));

        if (!f.getTool().getToolId().equals(order.getTool().getToolId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "File không thuộc tool của đơn này");
        }

        Path p = Paths.get(f.getFilePath());
        if (!p.isAbsolute()) p = storageRoot.resolve(p).normalize();
        if (!Files.exists(p)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File đã bị xóa hoặc chưa upload");
        }

        try {
            Resource res = new UrlResource(p.toUri());
            String filename = p.getFileName().toString();
            return new FileDownload(res, filename, MediaType.APPLICATION_OCTET_STREAM);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể đọc file");
        }
    }

    /** Lịch sử gia hạn + filter + paging */
    @Transactional(readOnly = true)
    public HistoryData history(Long orderId,
                               LocalDateTime from, LocalDateTime to,
                               BigDecimal min, BigDecimal max,
                               String sort, String dir,
                               int page, int size) {
        CustomerOrder order = loadOrderOr404(orderId);
        ensureOrderSuccess(order);

        LicenseAccount acc = order.getLicenseAccount();
        if (acc == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn chưa được cấp license.");
        }

        String sortProp = switch (sort) {
            case "amount" -> "amountPaid";
            case "newEnd" -> "newEndDate";
            default -> "renewDate";
        };
        Sort.Direction direction = "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), Math.max(1, size), Sort.by(direction, sortProp));

        Page<LicenseRenewLog> logsPage =
                renewRepo.search(acc.getLicenseAccountId(), from, to, min, max, pageable);

        return new HistoryData(order, order.getTool(), acc, logsPage);
    }

    /** Gia hạn theo license chọn */
    @Transactional
    public String renew(Long orderId, Long licenseId) {
        CustomerOrder order = loadOrderOr404(orderId);
        ensureOrderSuccess(order);

        LicenseAccount acc = order.getLicenseAccount();
        if (acc == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn chưa được cấp license.");
        }

        License lic = licenseRepo.findById(licenseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Gói license không hợp lệ."));
        if (!lic.getTool().getToolId().equals(order.getTool().getToolId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Gói license không thuộc tool này.");
        }

        int days = (lic.getDurationDays() != null) ? lic.getDurationDays() : 30;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime base = (acc.getEndDate() != null && acc.getEndDate().isAfter(now)) ? acc.getEndDate() : now;
        LocalDateTime newEnd = base.plusDays(days);

        acc.setEndDate(newEnd);
        if (acc.getStartDate() == null) acc.setStartDate(now);
        acc.setStatus(LicenseAccount.Status.ACTIVE);
        accRepo.save(acc);

        LicenseRenewLog log = new LicenseRenewLog();
        log.setLicenseAccount(acc);
        log.setRenewDate(now);
        log.setNewEndDate(newEnd);
        log.setAmountPaid(lic.getPrice() != null ? BigDecimal.valueOf(lic.getPrice()) : BigDecimal.ZERO);
        renewRepo.save(log);

        return "Gia hạn thành công " + days + " ngày · Hạn mới: " + newEnd;
    }

    /** Cập nhật username/password */
    @Transactional
    public UpdateResult updateAccount(Long orderId, String username, String password) {
        CustomerOrder order = loadOrderOr404(orderId);

        LicenseAccount acc = order.getLicenseAccount();
        if (acc == null) {
            return new UpdateResult(false, "Đơn chưa được cấp license.");
        }

        // Check theo loginMethod của Tool, không dùng acc.getLicense() nữa
        Tool tool = order.getTool();
        if (tool.getLoginMethod() == Tool.LoginMethod.TOKEN) {
            return new UpdateResult(false, "Tài khoản dùng TOKEN: không thể đổi username/password.");
        }

        username = username == null ? "" : username.trim();
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            return new UpdateResult(false,
                    "Username phải 3–100 ký tự, chỉ gồm chữ, số, dấu '.', '_' hoặc '-', và không có khoảng trắng.");
        }
        if (password == null || !PASSWORD_PATTERN.matcher(password).matches()) {
            return new UpdateResult(false,
                    "Mật khẩu phải ≥ 8 ký tự, có ít nhất 1 chữ thường, 1 CHỮ HOA, 1 chữ số và không có khoảng trắng (không bắt buộc ký tự đặc biệt).");
        }

        acc.setUsername(username);
        acc.setPassword(password);
        accRepo.save(acc);

        return new UpdateResult(true, "Đã cập nhật tài khoản đăng nhập.");
    }

    // ====================== DTOs gọn ======================
    public record ViewData(CustomerOrder order, Tool tool, LicenseAccount acc,
                           List<ToolFile> files, List<License> licenses,
                           String template) {}

    public record FileDownload(Resource resource, String filename, MediaType contentType) {}

    public record HistoryData(CustomerOrder order, Tool tool, LicenseAccount acc,
                              Page<LicenseRenewLog> logsPage) {}

    public record UpdateResult(boolean ok, String message) {}

    // ====================== HELPERS ======================
    private CustomerOrder loadOrderOr404(Long orderId) {
        return orderRepo.findByOrderId(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng"));
    }

    private void ensureOrderSuccess(CustomerOrder order) {
        if (order.getOrderStatus() != CustomerOrder.OrderStatus.SUCCESS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn hàng chưa thành công.");
        }
    }

    /** Gia hạn có gắn transaction (dùng cho thanh toán VNPay) */
    @Transactional
    public void renewWithTransaction(Long orderId, Long licenseId, WalletTransaction tx) {
        CustomerOrder order = loadOrderOr404(orderId);
        ensureOrderSuccess(order);

        LicenseAccount acc = order.getLicenseAccount();
        if (acc == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn chưa được cấp license.");
        }

        License lic = licenseRepo.findById(licenseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Gói license không hợp lệ."));
        if (!lic.getTool().getToolId().equals(order.getTool().getToolId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Gói license không thuộc tool này.");
        }

        int days = (lic.getDurationDays() != null) ? lic.getDurationDays() : 30;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime base = (acc.getEndDate() != null && acc.getEndDate().isAfter(now)) ? acc.getEndDate() : now;
        LocalDateTime newEnd = base.plusDays(days);

        // Cập nhật account
        acc.setEndDate(newEnd);
        if (acc.getStartDate() == null) acc.setStartDate(now);
        acc.setStatus(LicenseAccount.Status.ACTIVE);
        accRepo.save(acc);

        // Ghi log gia hạn
        LicenseRenewLog log = new LicenseRenewLog();
        log.setLicenseAccount(acc);
        log.setRenewDate(now);
        log.setNewEndDate(newEnd);

        // Ưu tiên số tiền từ transaction, nếu null thì fallback về price của license
        BigDecimal amountPaid =
                (tx != null && tx.getAmount() != null)
                        ? tx.getAmount()
                        : (lic.getPrice() != null ? BigDecimal.valueOf(lic.getPrice()) : BigDecimal.ZERO);
        log.setAmountPaid(amountPaid);

        // Nếu trong LicenseRenewLog bạn có field tham chiếu transaction thì set luôn ở đây
        // ví dụ:
        // log.setTransaction(tx);

        renewRepo.save(log);
    }
}
