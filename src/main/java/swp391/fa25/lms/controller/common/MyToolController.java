package swp391.fa25.lms.controller.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import swp391.fa25.lms.model.CustomerOrder;
import swp391.fa25.lms.repository.CustomerOrderRepository;

@Controller
public class MyToolController {

    @Autowired
    private CustomerOrderRepository orderRepo;

    /**
     * Khi người mua bấm “Dùng dịch vụ”
     * → /my-tools/{orderId} hiển thị thông tin license tương ứng
     */
    @GetMapping("/my-tools/{orderId}")
    public String useTool(@PathVariable Long orderId, Model model) {
        CustomerOrder order = orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));

        // Truy cập thông tin license từ order
        var tool = order.getTool();
        var loginMethod = tool.getLoginMethod();

        model.addAttribute("tool", tool);
        model.addAttribute("order", order);
        model.addAttribute("loginMethod", loginMethod);

        if (loginMethod == order.getTool().getLoginMethod().TOKEN) {
            model.addAttribute("token", order.getLicenseAccount().getToken());
            return "customer/mytool-token"; // Trang chỉ hiển thị token
        } else {
            model.addAttribute("username", order.getLicenseAccount().getUsername());
            model.addAttribute("password", order.getLicenseAccount().getPassword());
            return "customer/mytool-userpass"; // Trang hiển thị username/password
        }
    }
}
