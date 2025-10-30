package swp391.fa25.lms.controller.seller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import swp391.fa25.lms.service.seller.SellerFeedbackService;

import jakarta.validation.constraints.Size;
import java.security.Principal;

@Controller
@RequestMapping("/seller/feedback")
public class SellerFeedbackController {
    @Autowired
    @Qualifier("sellerFeedbackService")
    private final SellerFeedbackService sellerFeedbackService;

    // Dùng constructor injection + Qualifier, bỏ @Autowired field
    public SellerFeedbackController(SellerFeedbackService sellerFeedbackService) {
        this.sellerFeedbackService = sellerFeedbackService;
    }

    // Bắt cả "" và "/"
    @GetMapping({"", "/"})
    public String listFeedbacks(@RequestParam(required = false) Long toolId,
                                Model model,
                                Principal principal) {
        var list = sellerFeedbackService.listFeedbacks(toolId, principal);
        model.addAttribute("feedbacks", list);
        model.addAttribute("toolId", toolId);
        return "seller/feedback-list";
    }

    @PostMapping("/{feedbackId}/reply")
    public Object upsertReply(@PathVariable Long feedbackId,
                              @RequestParam @Size(max = 200) String content,
                              @RequestParam(required = false) Long toolId,
                              @RequestParam(required = false) String redirect, // <<< nhận redirect
                              @RequestHeader(value = "X-Requested-With", required = false) String xrw,
                              @RequestHeader(value = "Accept", required = false) String accept,
                              Principal principal) {

        var vm = sellerFeedbackService.upsertReply(feedbackId, content, principal);

        boolean isAjax = "XMLHttpRequest".equalsIgnoreCase(xrw)
                || (accept != null && accept.contains(MediaType.APPLICATION_JSON_VALUE));
        if (isAjax) {
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(vm);
        }
        // Ưu tiên redirect param; fallback về toolId nếu có; cuối cùng là /seller/feedback
        String fallback = (toolId != null) ? ("/seller/feedback?toolId=" + toolId) : "/seller/feedback";
        String target = (redirect != null && !redirect.isBlank()) ? redirect : fallback;
        return "redirect:" + target;
    }

    @PostMapping("/{feedbackId}/reply/delete")
    public Object deleteReply(@PathVariable Long feedbackId,
                              @RequestParam(required = false) Long toolId,
                              @RequestParam(required = false) String redirect, // <<< nhận redirect
                              @RequestHeader(value = "X-Requested-With", required = false) String xrw,
                              Principal principal) {
        sellerFeedbackService.deleteReply(feedbackId, principal);

        boolean isAjax = "XMLHttpRequest".equalsIgnoreCase(xrw);
        if (isAjax) {
            return ResponseEntity.ok().build();
        }
        String fallback = (toolId != null) ? ("/seller/feedback?toolId=" + toolId) : "/seller/feedback";
        String target = (redirect != null && !redirect.isBlank()) ? redirect : fallback;
        return "redirect:" + target;
    }

    // (Tùy chọn) endpoint test mapping nhanh
    @GetMapping("/ping")
    @ResponseBody
    public String ping() { return "ok"; }
}
