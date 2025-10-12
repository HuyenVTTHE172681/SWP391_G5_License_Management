package swp391.fa25.lms.controller.admin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.Role.RoleName;
import swp391.fa25.lms.repository.AccountRepository;
import swp391.fa25.lms.repository.RoleRepository;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class DashBoardAdminController {

    private static final String FIXED_ADMIN_EMAIL = "admin@gmail.com";

    private final AccountRepository accountRepo;
    private final RoleRepository roleRepository;

    public DashBoardAdminController(AccountRepository accountRepo, RoleRepository roleRepository) {
        this.accountRepo = accountRepo;
        this.roleRepository = roleRepository;
    }

    @GetMapping({"", "/"})
    public String home() {
        return "redirect:/admin/accounts";
    }
    @GetMapping("/accounts")
    public String list(
            @RequestParam(required = false, defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "accountId,asc") String sort, // ví dụ: email,desc
            Model model,
            @ModelAttribute("msg") String msg) {

        String[] s = sort.split(",");
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(s[1]), s[0]));

        Page<Account> accPage =
                (q == null || q.isBlank())
                        ? accountRepo.findAll(pageable)
                        : accountRepo.findByEmailContainingIgnoreCaseOrFullNameContainingIgnoreCase(q, q, pageable);

        model.addAttribute("accPage", accPage);
        model.addAttribute("q", q);
        model.addAttribute("sort", sort);
        model.addAttribute("roles", RoleName.values());
        model.addAttribute("msg", msg);
        model.addAttribute("fixedAdminEmail", FIXED_ADMIN_EMAIL);
        return "admin/accounts";
    }

    @GetMapping("/accounts/{id}")
    public String detail(@PathVariable long id, Model model, RedirectAttributes ra) {
        var opt = accountRepo.findById(id);
        if (opt.isEmpty()) {
            ra.addFlashAttribute("msg", "Account not found: " + id);
            return "redirect:/admin/accounts";
        }
        model.addAttribute("acc", opt.get());
        model.addAttribute("fixedAdminEmail", FIXED_ADMIN_EMAIL);
        return "admin/account-details";
    }

    @PostMapping("/accounts/{id}/role")
    public String changeRole(@PathVariable long id,
                             @RequestParam("role") RoleName newRole,
                             RedirectAttributes ra) {

        var accOpt = accountRepo.findById(id);
        if (accOpt.isEmpty()) {
            ra.addFlashAttribute("msg", "Account not found: " + id);
            return "redirect:/admin/accounts";
        }
        var roleOpt = roleRepository.findByRoleName(newRole);
        if (roleOpt.isEmpty()) {
            ra.addFlashAttribute("msg", "Role not found: " + newRole);
            return "redirect:/admin/accounts";
        }

        Account acc = accOpt.get();

        if (FIXED_ADMIN_EMAIL.equalsIgnoreCase(acc.getEmail()) && newRole != RoleName.ADMIN) {
            ra.addFlashAttribute("msg", "Cannot change role of the fixed admin account.");
            return "redirect:/admin/accounts";
        }
        if (!FIXED_ADMIN_EMAIL.equalsIgnoreCase(acc.getEmail()) && newRole == RoleName.ADMIN) {
            ra.addFlashAttribute("msg", "Only account " + FIXED_ADMIN_EMAIL + " can have ADMIN role.");
            return "redirect:/admin/accounts";
        }

        acc.setRole(roleOpt.get());
        accountRepo.save(acc);
        ra.addFlashAttribute("msg", "Updated role to " + newRole + " for account " + acc.getEmail());
        return "redirect:/admin/accounts";
    }

    @PostMapping("/accounts/{id}/delete")
    public String delete(@PathVariable long id, RedirectAttributes ra) {
        var accOpt = accountRepo.findById(id);
        if (accOpt.isEmpty()) {
            ra.addFlashAttribute("msg", "Account not found: " + id);
            return "redirect:/admin/accounts";
        }
        if (FIXED_ADMIN_EMAIL.equalsIgnoreCase(accOpt.get().getEmail())) {
            ra.addFlashAttribute("msg", "Cannot delete the fixed admin account.");
            return "redirect:/admin/accounts";
        }
        try {
            accountRepo.deleteById(id);
            ra.addFlashAttribute("msg", "Deleted account: " + id);
        } catch (Exception ex) {
            ra.addFlashAttribute("msg", "Cannot delete account " + id + " (in use).");
        }
        return "redirect:/admin/accounts";
    }
}
