package swp391.fa25.lms.controller.common;

import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import swp391.fa25.lms.model.Account;

@ControllerAdvice
public class GlobalModelAttributes {

    /**
     * Thêm account (nếu có) vào model cho tất cả các controller
     */
    @ModelAttribute
    public void addGlobalAttributes(HttpSession session, Model model) {
        Account account = (Account) session.getAttribute("loggedInAccount");
        if (account != null) {
            model.addAttribute("account", account);
        }
    }
}
