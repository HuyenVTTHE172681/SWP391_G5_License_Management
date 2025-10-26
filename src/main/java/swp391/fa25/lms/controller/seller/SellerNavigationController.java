package swp391.fa25.lms.controller.seller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
@Controller
public class SellerNavigationController {
    // 🧑‍💼 Trang View Profile
    @GetMapping("/seller/profile")
    public String goToProfile() {
        // Trả về tên trang (template) — dev frontend sẽ tạo file "profile.html"
        return "seller/profile";
    }

    // 💬 Trang View Feedback
    @GetMapping("/seller/feedback")
    public String goToFeedback() {
        // Trả về template "feedback.html" để team frontend xử lý sau
        return "seller/feedback";
    }
}
