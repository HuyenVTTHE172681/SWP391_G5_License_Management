package swp391.fa25.lms.controller.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import swp391.fa25.lms.config.CustomerUserDetail;
import swp391.fa25.lms.model.Account;

@Controller
public class HomePageController {

    @GetMapping("/")
    public String defaultRedirect() {
        // Mặc định chuyển hướng về /home
        return "redirect:/home";
    }

    @GetMapping("/home")
    public String showHomePage(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.getPrincipal() instanceof CustomerUserDetail userDetail) {
            // ✅ Lấy thông tin tài khoản từ UserDetails
            Account account = userDetail.getAccount();
            model.addAttribute("account", account);
            model.addAttribute("maskedPassword", "********");
        } else {
            // Trường hợp chưa đăng nhập
            model.addAttribute("account", null);
        }

        return "public/home";
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request) {
        // ✅ Xóa session và context bảo mật
        request.getSession().invalidate();
        SecurityContextHolder.clearContext();
        return "redirect:/login";
    }
}
