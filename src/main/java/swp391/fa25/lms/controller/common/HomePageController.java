package swp391.fa25.lms.controller.common;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomePageController {

    @GetMapping("/")
    public String homePage() {
        return "home"; // sẽ trả về file templates/home.html
    }
}
