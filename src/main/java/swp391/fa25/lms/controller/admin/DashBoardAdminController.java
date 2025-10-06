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

    private AccountRepo accountRepo;
    private RoleRepo roleRepo;

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
        model.addAttribute("roles", RoleName.values()); // cho dropdown đổi role
        model.addAttribute("msg", msg);
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
        acc.setRole(roleOpt.get());
        accountRepo.save(acc);
        ra.addFlashAttribute("msg", "Updated role to " + newRole);
        return "redirect:/admin/accounts";
    }

    // Delete account
    @PostMapping("/accounts/{id}/delete")
    public String delete(@PathVariable long id, RedirectAttributes ra) {
        if (!accountRepo.existsById(id)) {
            ra.addFlashAttribute("msg", "Account not found: " + id);
            return "redirect:/admin/accounts";
        }
        try {
            accountRepo.deleteById(id);
            ra.addFlashAttribute("msg", "Deleted account: " + id);
        } catch (Exception ex) {
            // Nếu dính ràng buộc FK (đơn hàng/feedback...), báo lỗi
            ra.addFlashAttribute("msg", "Cannot delete account " + id + " (in use).");
        }
        return "redirect:/admin/accounts";
    }
}
