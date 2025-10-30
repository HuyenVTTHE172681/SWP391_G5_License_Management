package swp391.fa25.lms.controller.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import swp391.fa25.lms.service.customer.MyToolService;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Controller
@RequestMapping("/my-tools")
public class MyToolController {
      @Autowired
      @Qualifier("myTool")
    private  MyToolService myToolService;

    public MyToolController(MyToolService myToolService) {
        this.myToolService = myToolService;
    }

    // ===== Trang chi tiết tool theo đơn hàng =====
    @GetMapping("/{orderId}")
    public String viewTool(@PathVariable Long orderId, Model model) {
        var vd = myToolService.viewTool(orderId);
        model.addAttribute("order", vd.order());
        model.addAttribute("tool", vd.tool());
        model.addAttribute("acc", vd.acc());
        model.addAttribute("files", vd.files());
        model.addAttribute("licenses", vd.licenses());
        return vd.template(); // "customer/mytool-token" | "customer/mytool-userpass"
    }

    // ===== Tải file ToolFile =====
    @GetMapping("/{orderId}/files/{fileId}")
    public ResponseEntity<Resource> download(@PathVariable Long orderId,
                                             @PathVariable Long fileId) {
        var fd = myToolService.download(orderId, fileId);
        return ResponseEntity.ok()
                .contentType(fd.contentType())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fd.filename() + "\"")
                .body(fd.resource());
    }

    // ===== Trang lịch sử gia hạn =====
    @GetMapping("/{orderId}/history")
    public String history(@PathVariable Long orderId,
                          @RequestParam(required = false)
                          @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME)
                          LocalDateTime from,
                          @RequestParam(required = false)
                          @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME)
                          LocalDateTime to,
                          @RequestParam(required = false) BigDecimal min,
                          @RequestParam(required = false) BigDecimal max,
                          @RequestParam(defaultValue = "renewDate") String sort,
                          @RequestParam(defaultValue = "desc") String dir,
                          @RequestParam(defaultValue = "1") int page,   // 1-based
                          @RequestParam(defaultValue = "10") int size,
                          Model model) {

        var hd = myToolService.history(orderId, from, to, min, max, sort, dir, page, size);

        model.addAttribute("order", hd.order());
        model.addAttribute("tool", hd.tool());
        model.addAttribute("acc", hd.acc());
        model.addAttribute("logsPage", hd.logsPage());

        // giữ lại giá trị form
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("min", min);
        model.addAttribute("max", max);
        model.addAttribute("sort", sort);
        model.addAttribute("dir", dir);
        model.addAttribute("size", size);
        model.addAttribute("page", page);

        var f = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
        model.addAttribute("fromStr", from != null ? from.format(f) : "");
        model.addAttribute("toStr",   to   != null ? to.format(f)   : "");

        return "customer/mytool-history";
    }

    // ===== Renew =====
    @PostMapping("/{orderId}/renew")
    public String renew(@PathVariable Long orderId,
                        @RequestParam Long licenseId,
                        RedirectAttributes ra) {
        String msg = myToolService.renew(orderId, licenseId);
        ra.addFlashAttribute("ok", msg);
        return "redirect:/my-tools/" + orderId;
    }

    // ===== Đổi username/password =====
    @PostMapping("/{orderId}/account")
    public String updateAccount(@PathVariable Long orderId,
                                @RequestParam String username,
                                @RequestParam String password,
                                RedirectAttributes ra) {
        var rs = myToolService.updateAccount(orderId, username, password);
        if (rs.ok()) ra.addFlashAttribute("ok", rs.message());
        else         ra.addFlashAttribute("err", rs.message());
        return "redirect:/my-tools/" + orderId;
    }
}
