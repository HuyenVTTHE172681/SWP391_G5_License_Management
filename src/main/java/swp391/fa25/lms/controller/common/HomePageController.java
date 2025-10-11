package swp391.fa25.lms.controller.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import swp391.fa25.lms.config.CustomerUserDetail;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.Category;
import swp391.fa25.lms.model.Tool;
import swp391.fa25.lms.repository.CategoryRepo;
import swp391.fa25.lms.repository.ToolRepo;
import swp391.fa25.lms.service.CategoryService;
import swp391.fa25.lms.service.ToolService;


import java.time.LocalDateTime;
import java.util.List;

@Controller
public class HomePageController {

    @Autowired
    private CategoryService categoryService;
    @Autowired
    private ToolService toolService;

    @GetMapping("/")
    public String defaultRedirect() {
        // Default => redirect tới /home
        return "redirect:/home";
    }

    // Home
    @GetMapping("/home")
    public String showHomePage(
            HttpServletRequest request,
            Model model) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Account account = (Account) request.getSession().getAttribute("loggedInAccount");

        if (auth != null && auth.getPrincipal() instanceof CustomerUserDetail userDetail) {
            // ✅ Lấy thông tin tài khoản từ UserDetails
            account = userDetail.getAccount();
            model.addAttribute("account", account);
            model.addAttribute("maskedPassword", "********");
        } else {
            // Trường hợp chưa đăng nhập
            model.addAttribute("account", null);
        }

//        if (auth != null && auth.getPrincipal() instanceof CustomerUserDetail userDetail) {
//            // ✅ Lấy thông tin tài khoản từ UserDetails
//            Account account = userDetail.getAccount();
//            model.addAttribute("account", account);
//            model.addAttribute("maskedPassword", "********");
//        } else {
//            // Trường hợp chưa đăng nhập
//            model.addAttribute("account", null);
//        }
//
//        return "public/home";

        // Add vao model
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("account", account);
        model.addAttribute("maskedPassword", request.getSession().getAttribute("maskedPassword"));
        return "public/home";
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request) {
        request.getSession().invalidate();
        return "redirect:/login";
    }
}
