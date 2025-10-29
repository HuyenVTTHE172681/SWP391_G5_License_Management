package swp391.fa25.lms.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriUtils;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.Role.RoleName;
import swp391.fa25.lms.service.admin.AdminAccountService;
import swp391.fa25.lms.service.admin.AdminHomeService;

import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class DashBoardAdminController {

    private static final String FIXED_ADMIN_EMAIL = "admin@gmail.com";
    @Autowired
    @Qualifier("adminHomeService")
    private final AdminHomeService adminHomeService;
    @Autowired
    @Qualifier("adminAccountService")
    private final AdminAccountService adminAccountService;

    public DashBoardAdminController(AdminHomeService adminHomeService,
                                    AdminAccountService adminAccountService) {
        this.adminHomeService = adminHomeService;
        this.adminAccountService = adminAccountService;
    }

    @GetMapping({"", "/"})
    public String root() {
        return "redirect:/admin/adminhome";
    }
    private static final java.util.Set<String> ALLOWED_SORT_FIELDS = java.util.Set.of(
            "accountId", "email", "fullName", "createdAt", "updatedAt"
    );
    private Sort parseSort(String sortStr, String fallbackProp, Sort.Direction fallbackDir) {
        // nếu chuỗi rỗng → dùng fallback
        if (!org.springframework.util.StringUtils.hasText(sortStr)) {
            return Sort.by(fallbackDir, fallbackProp);
        }

        String[] parts = sortStr.split(",", 2);
        String prop = parts.length > 0 && org.springframework.util.StringUtils.hasText(parts[0])
                ? parts[0].trim()
                : fallbackProp;

        // chỉ cho phép sort theo field hợp lệ
        if (!ALLOWED_SORT_FIELDS.contains(prop)) {
            prop = fallbackProp;
        }

        String dirStr = (parts.length > 1 && org.springframework.util.StringUtils.hasText(parts[1]))
                ? parts[1].trim()
                : fallbackDir.name();

        Sort.Direction dir;
        try {
            dir = Sort.Direction.fromString(dirStr);
        } catch (Exception e) {
            dir = fallbackDir;
        }

        return Sort.by(dir, prop);
    }

    // ===== Tab 1: HOME (KPI + 2 danh sách có search/sort/paging) =====
    @GetMapping("/adminhome")
    public String home(
            Model model,
            @ModelAttribute("msg") String msg,

            // Khối 1: Latest registrations
            @RequestParam(name = "q1", defaultValue = "") String q1,
            @RequestParam(name = "sort1", defaultValue = "createdAt,desc") String sort1,
            @RequestParam(name = "p1", defaultValue = "0") int p1,
            @RequestParam(name = "s1", defaultValue = "5") int s1,

            // Khối 2: Deactivated users
            @RequestParam(name = "q2", defaultValue = "") String q2,
            @RequestParam(name = "sort2", defaultValue = "updatedAt,desc") String sort2,
            @RequestParam(name = "p2", defaultValue = "0") int p2,
            @RequestParam(name = "s2", defaultValue = "5") int s2
    ) {

        // KPIs
        var kpis = adminHomeService.kpis();
        model.addAttribute("totalUsers", kpis.get("totalUsers"));
        model.addAttribute("totalSellers", kpis.get("totalSellers"));
        model.addAttribute("totalCustomers", kpis.get("totalCustomers"));

        // Parse sort1, sort2 (an toàn)
        Sort sortLatest = parseSort(sort1, "createdAt", Sort.Direction.DESC);
        Sort sortDeact  = parseSort(sort2, "updatedAt", Sort.Direction.DESC);

        // Pageable
        Pageable pageableLatest = PageRequest.of(Math.max(p1, 0), Math.max(s1, 1), sortLatest);
        Pageable pageableDeact  = PageRequest.of(Math.max(p2, 0), Math.max(s2, 1), sortDeact);

        // Gọi search chung bên AdminAccountService
        // Latest registrations: không filter status (null)
        Page<Account> accLatestPage = adminAccountService.search(q1, pageableLatest, null);

        // Deactivated: filter status = DEACTIVATED
        Page<Account> accDeactPage = adminAccountService.search(
                q2, pageableDeact, Account.AccountStatus.DEACTIVATED);

        // Model attributes
        model.addAttribute("accLatestPage", accLatestPage);
        model.addAttribute("accDeactPage", accDeactPage);

        model.addAttribute("q1", q1);
        model.addAttribute("sort1", sort1);
        model.addAttribute("p1", p1);
        model.addAttribute("s1", s1);

        model.addAttribute("q2", q2);
        model.addAttribute("sort2", sort2);
        model.addAttribute("p2", p2);
        model.addAttribute("s2", s2);

        model.addAttribute("fixedAdminEmail", FIXED_ADMIN_EMAIL);
        model.addAttribute("page", "home");

        if (StringUtils.hasText(msg)) {
            model.addAttribute("msg", msg);
        }
        return "admin/adminhome";
    }
    // ===== Tab 2: ACCOUNTS (CRUD + filter/search) =====
    @GetMapping("/accounts")
    public String list(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "ACTIVE") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "accountId,asc") String sort,
            Model model,
            @ModelAttribute("msg") String msg) {
        String[] s = sort.split(",");
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(s[1]), s[0]));
        Account.AccountStatus st = null;
        try { st = (status == null || status.isBlank()) ? null : Account.AccountStatus.valueOf(status); }
        catch (Exception ignored) {}

        Page<Account> accPage = adminAccountService.search(q, pageable, st);

        model.addAttribute("accPage", accPage);
        model.addAttribute("q", q);
        model.addAttribute("status", status);
        model.addAttribute("sort", sort);
        model.addAttribute("roles", RoleName.values());
        model.addAttribute("fixedAdminEmail", FIXED_ADMIN_EMAIL);
        model.addAttribute("page", "accounts");
        if (StringUtils.hasText(msg)) {
            model.addAttribute("msg", msg);
        }
        return "admin/accounts";
    }

    @GetMapping("/accounts/{id}")
    public String detail(@PathVariable long id, Model model, RedirectAttributes ra) {
        try {
            model.addAttribute("acc", adminAccountService.get(id));
            model.addAttribute("fixedAdminEmail", FIXED_ADMIN_EMAIL);
            model.addAttribute("page", "accounts");
            return "admin/account-details";
        } catch (Exception ex) {
            ra.addFlashAttribute("msg", ex.getMessage());
            return "redirect:/admin/accounts";
        }
    }

    @PostMapping("/accounts/{id}/role")
    public String changeRole(@PathVariable long id,
                             @RequestParam("role") RoleName newRole,
                             RedirectAttributes ra) {
        try {
            adminAccountService.changeRole(id, newRole);
            ra.addFlashAttribute("msg", "Updated role to " + newRole + " for account " + id);
        } catch (Exception ex) {
            ra.addFlashAttribute("msg", ex.getMessage());
        }
        return "redirect:/admin/accounts";
    }

    @PostMapping("/accounts/{id}/delete")
    public String delete(@PathVariable long id,
                         @RequestParam(defaultValue = "") String q,
                         @RequestParam(defaultValue = "ACTIVE") String status,
                         @RequestParam(defaultValue = "0") int page,
                         @RequestParam(defaultValue = "10") int size,
                         @RequestParam(defaultValue = "accountId,asc") String sort,
                         RedirectAttributes ra) {
        try {
            Account acc = adminAccountService.get(id);
            if (acc != null && FIXED_ADMIN_EMAIL.equalsIgnoreCase(acc.getEmail())) {
                ra.addFlashAttribute("msg", "Không thể vô hiệu hóa tài khoản ADMIN cố định.");
            } else {
                adminAccountService.deactivate(id);
                ra.addFlashAttribute("msg", "Đã chuyển tài khoản " + id + " sang DEACTIVATED.");
            }
        } catch (Exception ex) {
            ra.addFlashAttribute("msg", ex.getMessage());
        }
        return "redirect:/admin/accounts?q=" + UriUtils.encode(q, StandardCharsets.UTF_8)
                + "&status=" + status + "&page=" + page + "&size=" + size + "&sort=" + UriUtils.encode(sort, StandardCharsets.UTF_8);
    }

    @PostMapping("/accounts/{id}/reactivate")
    public String reactivate(@PathVariable long id,
                             RedirectAttributes ra) {
        try {
            adminAccountService.reactivate(id);
            ra.addFlashAttribute("msg", "Đã kích hoạt lại tài khoản " + id + ".");
        } catch (Exception ex) {
            ra.addFlashAttribute("msg", ex.getMessage());
        }
        return "redirect:/admin/adminhome";
    }
}
