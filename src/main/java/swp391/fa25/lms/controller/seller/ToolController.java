package swp391.fa25.lms.controller.seller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import swp391.fa25.lms.repository.CategoryRepository;
import swp391.fa25.lms.repository.ToolRepository;
import swp391.fa25.lms.repository.ToolRepository;

@Controller
public class ToolController {
    @Autowired
    private ToolRepository toolRepo;

    @Autowired
    private CategoryRepository categoryRepo;
}
