package swp391.fa25.lms.controller.seller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import swp391.fa25.lms.model.LicenseAccount;
import swp391.fa25.lms.model.Tool;
import swp391.fa25.lms.repository.LicenseAccountRepository;
import swp391.fa25.lms.repository.ToolRepository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/seller/tokens")
public class TokenController {

    @Autowired
    private ToolRepository toolRepository;

    @Autowired
    private LicenseAccountRepository licenseAccountRepository;

    /** 🔹 Trang quản lý token của tool */
    @GetMapping("/manage")
    public String manageTokens(@RequestParam("toolId") Long toolId, Model model) {
        Tool tool = toolRepository.findById(toolId)
                .orElseThrow(() -> new RuntimeException("Tool not found"));

        List<LicenseAccount> tokenAccounts =
                licenseAccountRepository.findByTool_ToolIdAndLoginMethod(toolId, LicenseAccount.LoginMethod.TOKEN);

        model.addAttribute("tool", tool);
        model.addAttribute("tokenAccounts", tokenAccounts);
        return "seller/token-manage";
    }

    /** 🔹 Thêm 1 token đơn lẻ (form input 1 dòng) */
    @PostMapping("/{toolId}/add")
    public String addSingleToken(@PathVariable Long toolId,
                                 @RequestParam("token") String token,
                                 RedirectAttributes redirectAttributes) {

        Tool tool = toolRepository.findById(toolId)
                .orElseThrow(() -> new RuntimeException("Tool not found"));

        // check trùng token
        if (licenseAccountRepository.existsByToken(token)) {
            redirectAttributes.addFlashAttribute("error", "Token already exists!");
            return "redirect:/seller/tokens/manage?toolId=" + toolId;
        }

        LicenseAccount acc = new LicenseAccount();
        acc.setTool(tool);
        acc.setLoginMethod(LicenseAccount.LoginMethod.TOKEN);
        acc.setToken(token);
        acc.setUsed(false);
        acc.setStatus(LicenseAccount.Status.ACTIVE);
        acc.setStartDate(LocalDateTime.now());
        acc.setOrder(null); // ✅ tránh lỗi null order_id

        licenseAccountRepository.save(acc);
        redirectAttributes.addFlashAttribute("success", "Added token successfully!");
        return "redirect:/seller/tokens/manage?toolId=" + toolId;
    }

    /** 🔹 Thêm nhiều token 1 lúc (textarea) */
    @PostMapping("/add-multiple")
    public String addMultipleTokens(@RequestParam("toolId") Long toolId,
                                    @RequestParam("tokens") String tokens,
                                    RedirectAttributes redirectAttributes) {

        Tool tool = toolRepository.findById(toolId)
                .orElseThrow(() -> new RuntimeException("Tool not found"));

        // Tách từng dòng, lọc dòng trống và bỏ trùng
        List<String> tokenList = Arrays.stream(tokens.split("\\r?\\n"))
                .map(String::trim)
                .filter(t -> !t.isEmpty())
                .distinct()
                .toList();

        int addedCount = 0;
        for (String tokenValue : tokenList) {
            if (licenseAccountRepository.existsByToken(tokenValue)) continue; // bỏ qua trùng

            LicenseAccount acc = new LicenseAccount();
            acc.setTool(tool);
            acc.setLoginMethod(LicenseAccount.LoginMethod.TOKEN);
            acc.setToken(tokenValue);
            acc.setUsed(false);
            acc.setStatus(LicenseAccount.Status.ACTIVE);
            acc.setOrder(null); // ✅ cho phép null order_id
            acc.setStartDate(LocalDateTime.now());
            licenseAccountRepository.save(acc);
            addedCount++;
        }

        redirectAttributes.addFlashAttribute("success", "Added " + addedCount + " tokens successfully!");
        return "redirect:/seller/tokens/manage?toolId=" + toolId;
    }

    /** 🔹 Xóa token */
    @PostMapping("/{id}/delete")
    public String deleteToken(@PathVariable Long id, @RequestParam("toolId") Long toolId,
                              RedirectAttributes redirectAttributes) {
        licenseAccountRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "Token deleted successfully!");
        return "redirect:/seller/tokens/manage?toolId=" + toolId;
    }
}
