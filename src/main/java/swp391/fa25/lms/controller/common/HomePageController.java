package swp391.fa25.lms.controller.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.service.CategoryService;

@Controller
public class HomePageController {

    @Autowired
    private CategoryService categoryService;

    @GetMapping("/")
    public String defaultRedirect() {
        // Default => redirect tới /home
        return "redirect:/home";
    }

    // Home
    @GetMapping("/home")
    public String showHomePage(HttpServletRequest request, Model model) {
        Account account = (Account) request.getSession().getAttribute("loggedInAccount");
//        if (account == null) return "redirect:/login";
        model.addAttribute("account", account);
        model.addAttribute("maskedPassword", request.getSession().getAttribute("maskedPassword"));
        model.addAttribute("categories", categoryService.getAllCategories());
        return "public/home";
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request) {
        request.getSession().invalidate();
        return "redirect:/login";
    }
}
