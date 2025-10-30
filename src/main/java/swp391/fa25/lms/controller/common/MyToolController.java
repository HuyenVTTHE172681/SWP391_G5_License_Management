package swp391.fa25.lms.controller.common;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import swp391.fa25.lms.model.*;
import swp391.fa25.lms.repository.*;
import java.util.regex.Pattern;

import java.math.BigDecimal;
import java.nio.file.*;
import java.time.LocalDateTime;


@Controller
@RequestMapping("/my-tools")
public class MyToolController {
    private static final Pattern USERNAME_PATTERN =
            Pattern.compile("^[A-Za-z0-9._-]{3,100}$");
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)\\S{8,}$");

    private final CustomerOrderRepository orderRepo;
    private final LicenseAccountRepository accRepo;
    private final LicenseRenewLogRepository renewRepo;
    private final ToolFileRepository fileRepo;
    private final LicenseToolRepository licenseRepo;

    // nơi bạn lưu file thực tế (đổi nếu cần)
    private final Path storageRoot = Paths.get("uploads");

    public MyToolController(CustomerOrderRepository orderRepo,
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

    private CustomerOrder loadOrderOr404(Long orderId) {
        return orderRepo.findByOrderId(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng"));
    }

    // ===== Trang chi tiết tool theo đơn hàng =====
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

        // Files theo Tool
        var files = fileRepo.findByTool_ToolIdOrderByCreatedAtDesc(order.getTool().getToolId());
        model.addAttribute("files", files);

        // Danh sách gói License theo Tool (model của bạn không có 'active')
        model.addAttribute("licenses", licenseRepo.findByToolToolId(order.getTool().getToolId()));

        var method = acc.getLoginMethod() != null
                ? acc.getLoginMethod()
                : (order.getTool().getLoginMethod() == Tool.LoginMethod.TOKEN
                ? LicenseAccount.LoginMethod.TOKEN
                : LicenseAccount.LoginMethod.USER_PASSWORD);

        return (method == LicenseAccount.LoginMethod.TOKEN)
                ? "customer/mytool-token"
                : "customer/mytool-userpass";
    }

    // ===== Tải file ToolFile =====
    @GetMapping("/{orderId}/files/{fileId}")
    @Transactional(readOnly = true)
    public ResponseEntity<Resource> download(@PathVariable Long orderId,
                                             @PathVariable Long fileId) {
        var order = loadOrderOr404(orderId);
        var f = fileRepo.findById(fileId)
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
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(res);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể đọc file");
        }
    }

    // ===== Trang lịch sử gia hạn =====
    @GetMapping("/{orderId}/history")
    @Transactional(readOnly = true)
    public String history(
            @PathVariable Long orderId,
            // bộ lọc:
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) BigDecimal min,
            @RequestParam(required = false) BigDecimal max,
            // sắp xếp + phân trang:
            @RequestParam(defaultValue = "renewDate") String sort,
            @RequestParam(defaultValue = "desc") String dir,
            @RequestParam(defaultValue = "1") int page,       // 1-based
            @RequestParam(defaultValue = "10") int size,
            Model model
    ) {
        var order = loadOrderOr404(orderId);
        if (order.getOrderStatus() != CustomerOrder.OrderStatus.SUCCESS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn hàng chưa thành công.");
        }
        var acc = order.getLicenseAccount();
        if (acc == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn chưa được cấp license.");

        // map tên sort thân thiện -> field entity
        String sortProp = switch (sort) {
            case "amount" -> "amountPaid";
            case "newEnd" -> "newEndDate";
            default -> "renewDate";
        };
        var direction = "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        var pageable = PageRequest.of(Math.max(0, page - 1), Math.max(1, size), Sort.by(direction, sortProp));

        var logsPage = renewRepo.search(acc.getLicenseAccountId(), from, to, min, max, pageable);

        model.addAttribute("order", order);
        model.addAttribute("tool", order.getTool());
        model.addAttribute("acc", acc);
        model.addAttribute("logsPage", logsPage);

        // giữ lại giá trị form
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("min", min);
        model.addAttribute("max", max);
        model.addAttribute("sort", sort);
        model.addAttribute("dir", dir);
        model.addAttribute("size", size);
        model.addAttribute("page", page);

        // giá trị string cho input datetime-local
        java.time.format.DateTimeFormatter f = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
        model.addAttribute("fromStr", from != null ? from.format(f) : "");
        model.addAttribute("toStr",   to   != null ? to.format(f)   : "");

        return "customer/mytool-history";
    }


    // ===== Renew: chọn gói cụ thể -> "thanh toán" giả lập -> cập nhật hạn + ghi log =====
    @PostMapping("/{orderId}/renew")
    @Transactional
    public String renew(@PathVariable Long orderId,
                        @RequestParam Long licenseId,           // nhận license đã chọn từ modal
                        RedirectAttributes ra) {
        var order = loadOrderOr404(orderId);
        if (order.getOrderStatus() != CustomerOrder.OrderStatus.SUCCESS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn chưa thành công.");
        }
        var acc = order.getLicenseAccount();
        if (acc == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn chưa được cấp license.");

        var lic = licenseRepo.findById(licenseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Gói license không hợp lệ."));
        if (!lic.getTool().getToolId().equals(order.getTool().getToolId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Gói license không thuộc tool này.");
        }

        // thanh toán giả lập: coi như success, lấy số ngày + giá từ gói
        int days = (lic.getDurationDays() != null) ? lic.getDurationDays() : 30;
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
        log.setAmountPaid(lic.getPrice() != null ? BigDecimal.valueOf(lic.getPrice()) : BigDecimal.ZERO);
        // nếu có transaction ví, set thêm log.setTransaction(...)
        renewRepo.save(log);

        ra.addFlashAttribute("ok", "Gia hạn thành công " + days + " ngày · Hạn mới: " + newEnd);
        return "redirect:/my-tools/" + orderId;
    }

    // ===== Đổi username/password (chỉ USER_PASSWORD) =====
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

        // --- VALIDATE ---
        username = username.trim();

        if (!USERNAME_PATTERN.matcher(username).matches()) {
            ra.addFlashAttribute("err",
                    "Username phải 3–100 ký tự, chỉ gồm chữ, số, dấu '.', '_' hoặc '-', và không có khoảng trắng.");
            return "redirect:/my-tools/" + orderId;
        }

        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            ra.addFlashAttribute("err",
                    "Mật khẩu phải ≥ 8 ký tự, có ít nhất 1 chữ thường, 1 CHỮ HOA, 1 chữ số và không có khoảng trắng (không bắt buộc ký tự đặc biệt).");
            return "redirect:/my-tools/" + orderId;
        }

        // TODO: nếu cần mã hoá: password = passwordEncoder.encode(password);
        acc.setUsername(username);
        acc.setPassword(password);
        accRepo.save(acc);

        ra.addFlashAttribute("ok", "Đã cập nhật tài khoản đăng nhập.");
        return "redirect:/my-tools/" + orderId;
    }
}
