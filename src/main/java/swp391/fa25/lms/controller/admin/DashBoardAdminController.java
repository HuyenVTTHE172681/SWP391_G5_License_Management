package swp391.fa25.lms.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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

    // ===== Tab 1: HOME (KPI + 2 danh sách) =====
    @GetMapping("/adminhome")
    public String home(Model model, @ModelAttribute("msg") String msg) {
        var kpis = adminHomeService.kpis();
        model.addAttribute("totalUsers", kpis.get("totalUsers"));
        model.addAttribute("totalSellers", kpis.get("totalSellers"));
        model.addAttribute("totalCustomers", kpis.get("totalCustomers"));

        model.addAttribute("latestAccounts", adminHomeService.latestAccounts(8));
        model.addAttribute("deactivatedAccounts", adminHomeService.deactivatedAccounts(8));

        model.addAttribute("fixedAdminEmail", FIXED_ADMIN_EMAIL);
        model.addAttribute("page", "home");
        model.addAttribute("msg", msg);
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
        model.addAttribute("msg", msg);
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
