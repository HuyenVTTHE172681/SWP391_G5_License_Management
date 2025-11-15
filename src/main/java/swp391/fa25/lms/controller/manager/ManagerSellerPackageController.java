package swp391.fa25.lms.controller.manager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import swp391.fa25.lms.model.SellerPackage;
import swp391.fa25.lms.service.manager.SellerPackageService;

import java.util.List;

@Controller
@RequestMapping("/manager/package")
public class ManagerSellerPackageController {

    @Autowired
    @Qualifier("managerSellerPackageService")
    private SellerPackageService service;

    @GetMapping("/list")
    public String listPackages(@RequestParam(required = false) String name,
                               @RequestParam(required = false) Double minPrice,
                               @RequestParam(required = false) Double maxPrice,
                               @RequestParam(required = false) Integer minDuration,
                               @RequestParam(required = false) Integer maxDuration,
                               @RequestParam(required = false) SellerPackage.Status status,
                               Model model) {

        List<SellerPackage> packages = service.filter(name, minPrice, maxPrice, minDuration, maxDuration, status);

        model.addAttribute("packages", packages);
        model.addAttribute("searchName", name);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("minDuration", minDuration);
        model.addAttribute("maxDuration", maxDuration);
        model.addAttribute("selectedStatus", status);
        return "manager/package-list";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("sellerPackage", new SellerPackage());
        return "manager/package-create";
    }

    @PostMapping("/create")
    public String createPackage(@Valid @ModelAttribute("sellerPackage") SellerPackage sellerPackage,
                                BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "manager/package-create";
        }
        if (service.isExist(sellerPackage.getPackageName())) {
            model.addAttribute("errMsg", "Package Name already exists");
            model.addAttribute("sellerPackage", sellerPackage);
            return "manager/package-create";
        }
        sellerPackage.setStatus(SellerPackage.Status.ACTIVE);
        service.save(sellerPackage);
        redirectAttributes.addFlashAttribute("message", "New Package Created");
        return "redirect:/manager/package/list";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable("id") int id, Model model) {
        SellerPackage sellerPackage = service.findById(id);
        model.addAttribute("sellerPackage", sellerPackage);
        return "manager/package-edit";
    }

    @PostMapping("/edit/{id}")
    public String updatePackage(@PathVariable("id") int id,
                                @Valid @ModelAttribute("sellerPackage") SellerPackage sellerPackage,
                                BindingResult result,
                                RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "manager/package-edit";
        }
        sellerPackage.setId(id);
        service.save(sellerPackage);
        redirectAttributes.addFlashAttribute("message", "Package Updated");
        return "redirect:/manager/package/list";
    }

    @GetMapping("/deactivate/{id}")
    public String deactivate(@PathVariable("id") int id, RedirectAttributes redirectAttributes) {
        SellerPackage sp = service.findById(id);
        sp.setStatus(SellerPackage.Status.DEACTIVATED);
        service.save(sp);
        redirectAttributes.addFlashAttribute("message", "Package Deactivated");
        return "redirect:/manager/package/list";
    }

    @GetMapping("/restore/{id}")
    public String restore(@PathVariable("id") int id,  RedirectAttributes redirectAttributes) {
        SellerPackage sp = service.findById(id);
        sp.setStatus(SellerPackage.Status.ACTIVE);
        service.save(sp);
        redirectAttributes.addFlashAttribute("message", "Package Restored");
        return "redirect:/manager/package/list";
    }
}
