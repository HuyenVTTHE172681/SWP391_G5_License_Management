package swp391.fa25.lms.controller.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import swp391.fa25.lms.model.LicenseAccount;
import swp391.fa25.lms.model.Tool;
import swp391.fa25.lms.service.customer.DemoLoginService;
import swp391.fa25.lms.service.manager.ToolService;

import java.time.LocalDateTime;
import java.util.Optional;

@Controller
@RequestMapping("/demo")
public class DemoLogin {
    @Autowired
    DemoLoginService demoLoginService;
    @Autowired
    ToolService toolService;

    @GetMapping("/{toolId}")
    public String showDemoLogin(@PathVariable Long toolId, Model model) {
        Tool tool = toolService.getToolById(toolId);

        if (tool == null) {
            model.addAttribute("error", "Tool not found");
            return "common/demo-login";
        }

        model.addAttribute("toolId", toolId);
        model.addAttribute("tool", tool);

        return "common/demo-login";
    }

    @PostMapping("/{toolId}")
    public String handleDemoLogin(@PathVariable Long toolId,
                                  @RequestParam(value = "token", required = false) String token,
                                  @RequestParam(value = "username", required = false) String username,
                                  @RequestParam(value = "password", required = false) String password,
                                  Model model) {

        Tool tool = toolService.getToolById(toolId);
        if (tool == null) {
            model.addAttribute("error", "Tool not found");
            return "common/demo-login";
        }
        if (!tool.getStatus().equals(Tool.Status.PUBLISHED)) {
            model.addAttribute("error", "Tool is not available");
            return "common/demo-login";
        }

        model.addAttribute("toolId", toolId);
        model.addAttribute("tool", tool);

        Optional<LicenseAccount> opt;

        if (tool.getLoginMethod() == Tool.LoginMethod.TOKEN) {
            if (token == null || token.isBlank()) {
                model.addAttribute("error", "Please input token to continue.");
                return "common/demo-login";
            }
            opt = demoLoginService.findByTokenAndTool(token.trim(), toolId);
            if (opt.isEmpty()) {
                model.addAttribute("error", "Incorrect token input.");
                return "common/demo-login";
            }

        } else {
            if (username == null || username.isBlank() ||
                    password == null || password.isBlank()) {
                model.addAttribute("error", "Username and password are required.");
                return "common/demo-login";
            }
            opt = demoLoginService.findByUsernamePasswordAndTool(
                    username.trim(), password, toolId
            );
            if (opt.isEmpty()) {
                model.addAttribute("error", "Incorrect username or password.");
                return "common/demo-login";
            }
        }

        LicenseAccount acc = opt.get();

        boolean isExpiredByStatus = acc.getStatus() == LicenseAccount.Status.EXPIRED;
        boolean isExpiredByDate = acc.getEndDate() != null
                && acc.getEndDate().isBefore(LocalDateTime.now());

        if (isExpiredByStatus || isExpiredByDate) {
            model.addAttribute("expired", true);
            model.addAttribute("message", "License expired. Please renew your license.");
            model.addAttribute("license", acc);
            return "common/demo-login";
        }

        if (acc.getStatus() != LicenseAccount.Status.ACTIVE) {
            model.addAttribute("error", "License is not active (status: " + acc.getStatus() + ").");
            return "common/demo-login";
        }

        model.addAttribute("license", acc);
        model.addAttribute("success", true);
        model.addAttribute("message", "License is active. Demo login successful.");

        return "common/demo";
    }
}



