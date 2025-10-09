package swp391.fa25.lms.controller.seller;

import org.springframework.ui.Model;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import swp391.fa25.lms.model.Category;
import swp391.fa25.lms.model.Tool;
import swp391.fa25.lms.repository.CategoryRepo;
import swp391.fa25.lms.repository.ToolRepo;

import java.time.LocalDateTime;
import java.util.List;

@Controller
public class ToolController {
    @Autowired
    private ToolRepo toolRepo;

    @Autowired
    private CategoryRepo categoryRepo;
}
