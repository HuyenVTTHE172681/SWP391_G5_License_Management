package swp391.fa25.lms.controller.common;

import org.springframework.web.bind.annotation.GetMapping;

public class HomePageController {

    @GetMapping("/")
    public String homePage() {
        return "home";
    }
}
