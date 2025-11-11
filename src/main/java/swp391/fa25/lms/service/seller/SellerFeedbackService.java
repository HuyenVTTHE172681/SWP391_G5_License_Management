package swp391.fa25.lms.service.seller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.Feedback;
import swp391.fa25.lms.model.FeedbackReply;
import swp391.fa25.lms.repository.AccountRepository;
import swp391.fa25.lms.repository.FeedBackReplyRepository;
import swp391.fa25.lms.repository.FeedbackRepository;
import swp391.fa25.lms.service.customer.ToolService;

import jakarta.validation.constraints.Size;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service("sellerFeedbackService")
public class SellerFeedbackService {

    public record ReplyVM(Long feedbackId, Long replyId, String sellerName, String content, String updatedAt) {}

    @Autowired
    private FeedbackRepository feedbackRepo;
    @Autowired
    private FeedBackReplyRepository replyRepo;
    @Autowired
    private ToolService toolService;
    @Autowired
    private AccountRepository accountRepo;

    public SellerFeedbackService(FeedbackRepository feedbackRepo,
                                 FeedBackReplyRepository replyRepo,
                                 ToolService toolService,
                                 AccountRepository accountRepo) {
        this.feedbackRepo = feedbackRepo;
        this.replyRepo = replyRepo;
        this.toolService = toolService;
        this.accountRepo = accountRepo;
    }

    // ================= LIST (giữ nguyên logic cũ) =================
    @Transactional(readOnly = true)
    public List<Feedback> listFeedbacks(Long toolId, Principal principal) {
        Account seller = getCurrentSeller(principal);

        if (toolId == null) {
            return feedbackRepo.findAll().stream()
                    .filter(f -> f.getTool() != null
                            && f.getTool().getSeller() != null
                            && f.getTool().getSeller().getAccountId().equals(seller.getAccountId()))
                    .toList();
        }

        var tool = toolService.findById(toolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return feedbackRepo.findByTool(tool).stream()
                .filter(f -> f.getTool() != null
                        && f.getTool().getSeller() != null
                        && f.getTool().getSeller().getAccountId().equals(seller.getAccountId()))
                .toList();
    }

    // ================= UPSERT REPLY =================
    @Transactional
    public ReplyVM upsertReply(Long feedbackId,
                               @Size(max = 200) String content,
                               Principal principal) {
        Account seller = getCurrentSeller(principal);

        var fb = feedbackRepo.findById(feedbackId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy feedback"));

        if (fb.getTool() == null
                || fb.getTool().getSeller() == null
                || !fb.getTool().getSeller().getAccountId().equals(seller.getAccountId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không có quyền.");
        }

        // ✅ CHẶN REPLY nếu feedback không ở trạng thái PUBLISHED
        if (fb.getStatus() != null) {
            String st = fb.getStatus().toString();
            if (!"PUBLISHED".equalsIgnoreCase(st)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Feedback này không ở trạng thái hiển thị, bạn không thể trả lời."
                );
            }
        }

        String body = content == null ? "" : content.trim();
        if (body.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nội dung phản hồi không được rỗng.");
        }
        if (isSpacedOutText(body)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Nội dung phản hồi không hợp lệ: không chèn khoảng trắng giữa từng ký tự.");
        }

        var existing = replyRepo.findByFeedback_FeedbackId(feedbackId).orElse(null);
        FeedbackReply saved;
        if (existing == null) {
            var rp = new FeedbackReply();
            rp.setFeedback(fb);
            rp.setSeller(seller);
            rp.setContent(body);
            rp.setCreatedAt(LocalDateTime.now());
            rp.setUpdatedAt(LocalDateTime.now());
            saved = replyRepo.save(rp);
        } else {
            existing.setContent(body);
            existing.setUpdatedAt(LocalDateTime.now());
            saved = replyRepo.save(existing);
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return new ReplyVM(
                fb.getFeedbackId(),
                saved.getReplyId(),
                seller.getFullName() != null ? seller.getFullName() : "Seller",
                saved.getContent(),
                saved.getUpdatedAt() != null ? saved.getUpdatedAt().format(fmt) : ""
        );
    }

    // ================= DELETE REPLY =================
    @Transactional
    public void deleteReply(Long feedbackId, Principal principal) {
        Account seller = getCurrentSeller(principal);

        var fb = feedbackRepo.findById(feedbackId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (fb.getTool() == null
                || fb.getTool().getSeller() == null
                || !fb.getTool().getSeller().getAccountId().equals(seller.getAccountId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        // ✅ CHẶN XOÁ REPLY nếu feedback không ở trạng thái PUBLISHED
        if (fb.getStatus() != null) {
            String st = fb.getStatus().toString();
            if (!"PUBLISHED".equalsIgnoreCase(st)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Feedback này không ở trạng thái hiển thị, bạn không thể xoá phản hồi."
                );
            }
        }

        replyRepo.deleteByFeedback_FeedbackId(feedbackId);
    }

    // ================= HELPERS =================
    private Account getCurrentSeller(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bạn cần đăng nhập.");
        }
        String email = principal.getName();
        Long accId = accountRepo.findIdByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không tìm thấy tài khoản."));
        return accountRepo.findById(accId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không tìm thấy tài khoản."));
    }

    private static boolean isSpacedOutText(String s) {
        if (s == null) return false;
        String t = s.trim().replaceAll("\\s+", " ");
        if (t.length() < 9) return false;
        String[] parts = t.split(" ");
        if (parts.length < 5) return false;
        for (String p : parts) if (!p.matches("[\\p{L}\\p{N}]")) return false;
        return true;
    }

    @Transactional(readOnly = true)
    public Optional<FeedbackReply> getReplyByFeedbackId(Long feedbackId) {
        if (feedbackId == null) return Optional.empty();
        return replyRepo.findByFeedback_FeedbackId(feedbackId);
    }

    @Transactional(readOnly = true)
    public Map<Long, List<FeedbackReply>> buildRepliesMapForFeedbacks(List<Feedback> feedbacks) {
        if (feedbacks == null || feedbacks.isEmpty()) return Collections.emptyMap();
        List<Long> ids = feedbacks.stream()
                .map(Feedback::getFeedbackId)
                .filter(Objects::nonNull)
                .toList();
        return buildRepliesMapForIds(ids);
    }

    @Transactional(readOnly = true)
    public Map<Long, List<FeedbackReply>> buildRepliesMapForIds(Collection<Long> feedbackIds) {
        if (feedbackIds == null || feedbackIds.isEmpty()) return Collections.emptyMap();

        List<FeedbackReply> replies = replyRepo.findByFeedback_FeedbackIdIn(feedbackIds);

        replies.sort(Comparator.comparing(FeedbackReply::getCreatedAt,
                Comparator.nullsLast(Comparator.naturalOrder())));

        Map<Long, List<FeedbackReply>> map = new LinkedHashMap<>();
        for (Long id : feedbackIds) map.put(id, new ArrayList<>());

        for (FeedbackReply r : replies) {
            if (r.getFeedback() == null || r.getFeedback().getFeedbackId() == null) continue;
            Long fid = r.getFeedback().getFeedbackId();
            map.computeIfAbsent(fid, k -> new ArrayList<>()).add(r);
        }
        return map;
    }

    @Transactional(readOnly = true)
    public List<ReplyVM> toVMs(Collection<FeedbackReply> list) {
        if (list == null || list.isEmpty()) return List.of();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return list.stream().map(r -> new ReplyVM(
                r.getFeedback() != null ? r.getFeedback().getFeedbackId() : null,
                r.getReplyId(),
                r.getSeller() != null && r.getSeller().getFullName() != null ? r.getSeller().getFullName() : "Seller",
                r.getContent(),
                r.getUpdatedAt() != null ? r.getUpdatedAt().format(fmt) :
                        (r.getCreatedAt() != null ? r.getCreatedAt().format(fmt) : "")
        )).collect(Collectors.toList());
    }

    // ================= Filters + Search =================
    public record Filters(
            Long toolId,
            String q,
            Integer ratingMin,
            Integer ratingMax,
            Boolean hasReply,   // null = tất cả; true = đã có reply; false = chưa reply
            LocalDate from,
            LocalDate to,
            String sort,        // "createdAt" | "rating"
            String dir,         // "asc" | "desc"
            int page,
            int size,
            String status       // ✅ filter theo status
    ) {}

    public record FeedbackPageVM(Page<Feedback> page, Map<Long, List<FeedbackReply>> repliesMap) {}

    @Transactional(readOnly = true)
    public FeedbackPageVM searchFeedbacks(Filters f, Principal principal) {
        Account seller = getCurrentSeller(principal);

        // 1) Lấy dữ liệu cơ sở theo seller (+ toolId nếu có)
        List<Feedback> base = (f.toolId() == null)
                ? feedbackRepo.findByTool_Seller_AccountIdOrderByCreatedAtDesc(seller.getAccountId())
                : feedbackRepo.findByTool_Seller_AccountIdAndTool_ToolIdOrderByCreatedAtDesc(
                seller.getAccountId(), f.toolId());

        // 2) Tập feedback có reply (dùng cho filter hasReply)
        Set<Long> allIds = base.stream()
                .map(Feedback::getFeedbackId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Long> idsWithReply = allIds.isEmpty()
                ? Collections.emptySet()
                : replyRepo.findByFeedback_FeedbackIdIn(allIds).stream()
                .map(fr -> fr.getFeedback() != null ? fr.getFeedback().getFeedbackId() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 3) Lọc trong bộ nhớ
        String kw = normalize(f.q());
        Integer rMin = f.ratingMin();
        Integer rMax = f.ratingMax();
        LocalDate from = f.from();
        LocalDate to = f.to();
        Boolean hasReply = f.hasReply();

        // ✅ chuẩn hoá status filter về UPPERCASE và để vào biến final
        String rawStatus = f.status();
        final String statusFilter;
        if (rawStatus == null) {
            statusFilter = null;
        } else {
            String t = rawStatus.trim();
            statusFilter = t.isEmpty() ? null : t.toUpperCase();
        }

        List<Feedback> filtered = base.stream().filter(fb -> {
            // hasReply
            if (hasReply != null) {
                boolean ok = idsWithReply.contains(fb.getFeedbackId());
                if (hasReply != ok) return false;
            }

            // status
            if (statusFilter != null) {
                if (fb.getStatus() == null) return false;
                String fbStatus = fb.getStatus().toString().toUpperCase();
                if (!fbStatus.equals(statusFilter)) return false;
            }

            // rating
            if (rMin != null && (fb.getRating() == null || fb.getRating() < rMin)) return false;
            if (rMax != null && (fb.getRating() == null || fb.getRating() > rMax)) return false;

            // date range theo createdAt
            if (from != null && (fb.getCreatedAt() == null || fb.getCreatedAt().isBefore(from.atStartOfDay()))) return false;
            if (to != null && (fb.getCreatedAt() == null || !fb.getCreatedAt().isBefore(to.plusDays(1).atStartOfDay()))) return false;

            // keyword: comment | tên KH | email | tên tool
            if (kw != null) {
                String cmt   = safeLower(fb.getComment());
                String name  = (fb.getAccount() != null) ? safeLower(fb.getAccount().getFullName()) : "";
                String mail  = (fb.getAccount() != null) ? safeLower(fb.getAccount().getEmail())     : "";
                String tname = (fb.getTool()    != null) ? safeLower(fb.getTool().getToolName())     : "";
                boolean hit = (cmt.contains(kw) || name.contains(kw) || mail.contains(kw) || tname.contains(kw));
                if (!hit) return false;
            }
            return true;
        }).collect(Collectors.toList());

        // 4) Sort trong bộ nhớ
        Comparator<Feedback> cmp =
                "rating".equalsIgnoreCase(f.sort())
                        ? Comparator.comparing(Feedback::getRating, Comparator.nullsLast(Comparator.naturalOrder()))
                        : Comparator.comparing(Feedback::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));

        if (!"asc".equalsIgnoreCase(f.dir())) {
            cmp = cmp.reversed();
        }
        filtered.sort(cmp);

        // 5) Phân trang trong bộ nhớ
        Pageable pageable = buildPageable(f.sort(), f.dir(), f.page(), f.size());
        int total = filtered.size();
        int fromIdx = (int) pageable.getOffset();
        int toIdx = Math.min(fromIdx + pageable.getPageSize(), total);
        List<Feedback> slice = (fromIdx >= total) ? List.of() : filtered.subList(fromIdx, toIdx);

        Page<Feedback> page = new PageImpl<>(slice, pageable, total);

        // 6) repliesMap chỉ cho các feedback ở trang hiện tại
        Set<Long> pageIds = slice.stream().map(Feedback::getFeedbackId).collect(Collectors.toSet());
        Map<Long, List<FeedbackReply>> repMap = pageIds.isEmpty()
                ? Map.of()
                : replyRepo.findByFeedback_FeedbackIdIn(pageIds).stream()
                .collect(Collectors.groupingBy(fr -> fr.getFeedback().getFeedbackId()));

        return new FeedbackPageVM(page, repMap);
    }

    private Pageable buildPageable(String sort, String dir, int page, int size) {
        String sortField = ("rating".equalsIgnoreCase(sort)) ? "rating" : "createdAt";
        Sort s = "asc".equalsIgnoreCase(dir) ? Sort.by(sortField).ascending() : Sort.by(sortField).descending();
        if (page < 0) page = 0;
        if (size <= 0 || size > 100) size = 10;
        // (Sort chủ yếu để client thấy meta; thực tế đã sort trong bộ nhớ)
        return PageRequest.of(page, size, s);
    }

    private static String normalize(String s) {
        if (s == null) return null;
        String t = s.trim().toLowerCase();
        return t.isEmpty() ? null : t;
    }

    private static String safeLower(String s) {
        return (s == null) ? "" : s.toLowerCase();
    }
}
