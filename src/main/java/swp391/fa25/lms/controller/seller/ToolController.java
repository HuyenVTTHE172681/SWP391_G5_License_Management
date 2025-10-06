package swp391.fa25.lms.controller.seller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import swp391.fa25.lms.model.Tool;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.service.ToolService;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/seller/tools")
public class ToolController {

    @Autowired
    private ToolService toolService;

    @GetMapping
    public String listTools(Model model, Principal principal) {
        Account seller = getCurrentSeller(principal);
        List<Tool> tools = toolService.getToolsBySeller(seller);
        model.addAttribute("tools", tools);
        return "seller/tool-list"; // thymeleaf page
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("tool", new Tool());
        return "seller/tool-add";
    }

    @PostMapping("/add")
    public String addTool(@ModelAttribute Tool tool, Principal principal) {
        Account seller = getCurrentSeller(principal);
        toolService.addTool(tool, seller);
        return "redirect:/seller/tools";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        model.addAttribute("toolId", id);
        return "seller/tool-edit";
    }

    @PostMapping("/edit/{id}")
    public String updateTool(@PathVariable Long id, @ModelAttribute Tool tool, Principal principal) {
        Account seller = getCurrentSeller(principal);
        toolService.updateTool(id, tool, seller);
        return "redirect:/seller/tools";
    }

    @GetMapping("/delete/{id}")
    public String deleteTool(@PathVariable Long id, Principal principal) {
        Account seller = getCurrentSeller(principal);
        toolService.deleteTool(id, seller);
        return "redirect:/seller/tools";
    }

    // ✅ Giả sử bạn có sẵn phương thức lấy seller từ principal (sẽ sửa sau khi có login)
    private Account getCurrentSeller(Principal principal) {
        Account seller = new Account();
        seller.setAccountId(1L);
        return seller;
    }
}
