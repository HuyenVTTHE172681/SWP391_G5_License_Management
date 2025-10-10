package swp391.fa25.lms.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.Role.RoleName;
import swp391.fa25.lms.repository.AccountRepo;
import swp391.fa25.lms.repository.RoleRepo;

@Controller
@RequestMapping("/admin")
public class DashBoardAdminController {

    private static final String FIXED_ADMIN_EMAIL = "admin@gmail.com";

    private final AccountRepo accountRepo;
    private final RoleRepo roleRepo;

    public DashBoardAdminController(AccountRepo accountRepo, RoleRepo roleRepo) {
        this.accountRepo = accountRepo;
        this.roleRepo = roleRepo;
    }

    @GetMapping({"", "/"})
    public String home() {
        return "redirect:/admin/accounts";
    }

    @GetMapping("/accounts")
    public String list(Model model, @ModelAttribute("msg") String msg) {
        model.addAttribute("accounts", accountRepo.findAll());
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
        return "admin/account-detail";
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
        var roleOpt = roleRepo.findByRoleName(newRole);
        if (roleOpt.isEmpty()) {
            ra.addFlashAttribute("msg", "Role not found: " + newRole);
            return "redirect:/admin/accounts";
        }

        Account acc = accOpt.get();

        //  Không cho đổi role của admin cố định sang role khác
        if (FIXED_ADMIN_EMAIL.equalsIgnoreCase(acc.getEmail()) && newRole != RoleName.ADMIN) {
            ra.addFlashAttribute("msg", "Cannot change role of the fixed admin account.");
            return "redirect:/admin/accounts";
        }

        //  Không cho promote người khác thành ADMIN
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

        // Không cho xóa admin cố định
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
