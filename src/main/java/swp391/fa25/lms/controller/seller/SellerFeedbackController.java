package swp391.fa25.lms.controller.seller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import swp391.fa25.lms.service.seller.SellerFeedbackService;

import jakarta.validation.constraints.Size;
import java.security.Principal;
import java.time.LocalDate;

@Controller
@RequestMapping("/seller/feedback")
public class SellerFeedbackController {

    private final SellerFeedbackService sellerFeedbackService;

    // Dùng constructor injection gọn gàng
    public SellerFeedbackController(SellerFeedbackService sellerFeedbackService) {
        this.sellerFeedbackService = sellerFeedbackService;
    }

    // ======= LIST + FILTER + SEARCH + PAGINATION =======
    @GetMapping({"", "/"})
    public String listFeedbacks(
            @RequestParam(required = false) Long toolId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer ratingMin,
            @RequestParam(required = false) Integer ratingMax,
            @RequestParam(required = false) Boolean hasReply,     // true/false/null
            @RequestParam(required = false) String from,          // yyyy-MM-dd
            @RequestParam(required = false) String to,            // yyyy-MM-dd
            @RequestParam(defaultValue = "createdAt") String sort, // createdAt|rating
            @RequestParam(defaultValue = "desc") String dir,       // asc|desc
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model,
            Principal principal
    ) {
        LocalDate fromDate = parseLocalDate(from);
        LocalDate toDate   = parseLocalDate(to);

        var filters = new SellerFeedbackService.Filters(
                toolId, q, ratingMin, ratingMax, hasReply,
                fromDate, toDate, sort, dir, page, size
        );

        var vm = sellerFeedbackService.searchFeedbacks(filters, principal);

        // Data cho view
        model.addAttribute("page", vm.page());                      // Page<Feedback>
        model.addAttribute("feedbacks", vm.page().getContent());    // tiện nếu view đang dùng 'feedbacks'
        model.addAttribute("repliesMap", vm.repliesMap());          // Map<Long, List<FeedbackReply>>

        // Giữ lại filter values để fill form & build paging links
        model.addAttribute("toolId", toolId);
        model.addAttribute("q", q);
        model.addAttribute("ratingMin", ratingMin);
        model.addAttribute("ratingMax", ratingMax);
        model.addAttribute("hasReply", hasReply);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("sort", sort);
        model.addAttribute("dir", dir);
        model.addAttribute("size", size);

        return "seller/feedback-list";
    }

    // ======= UPSERT REPLY =======
    @PostMapping("/{feedbackId}/reply")
    public Object upsertReply(@PathVariable Long feedbackId,
                              @RequestParam @Size(max = 200) String content,
                              @RequestParam(required = false) Long toolId,
                              @RequestParam(required = false) String redirect,
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

        String fallback = (toolId != null) ? ("/seller/feedback?toolId=" + toolId) : "/seller/feedback";
        String target = (redirect != null && !redirect.isBlank()) ? redirect : fallback;
        return "redirect:" + target;
    }

    // ======= DELETE REPLY =======
    @PostMapping("/{feedbackId}/reply/delete")
    public Object deleteReply(@PathVariable Long feedbackId,
                              @RequestParam(required = false) Long toolId,
                              @RequestParam(required = false) String redirect,
                              @RequestHeader(value = "X-Requested-With", required = false) String xrw,
                              Principal principal) {

        sellerFeedbackService.deleteReply(feedbackId, principal);

        boolean isAjax = "XMLHttpRequest".equalsIgnoreCase(xrw);
        if (isAjax) return ResponseEntity.ok().build();

        String fallback = (toolId != null) ? ("/seller/feedback?toolId=" + toolId) : "/seller/feedback";
        String target = (redirect != null && !redirect.isBlank()) ? redirect : fallback;
        return "redirect:" + target;
    }

    // (Tùy chọn) kiểm tra nhanh route
    @GetMapping("/ping")
    @ResponseBody
    public String ping() { return "ok"; }

    // ======= helpers =======
    private static LocalDate parseLocalDate(String s) {
        try {
            return (s == null || s.isBlank()) ? null : LocalDate.parse(s);
        } catch (Exception ignored) {
            return null;
        }
    }
}
