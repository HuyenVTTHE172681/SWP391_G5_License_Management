package swp391.fa25.lms.service.customer;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.fa25.lms.model.FeedbackReply;
import swp391.fa25.lms.repository.FeedBackReplyRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class FeedbackReadService {

    private final FeedBackReplyRepository replyRepo;

    public FeedbackReadService(FeedBackReplyRepository replyRepo) {
        this.replyRepo = replyRepo;
    }

    /** Trả về Map<feedbackId, List<FeedbackReply>> để template dùng thẳng */
    @Transactional(readOnly = true)
    public Map<Long, List<FeedbackReply>> mapRepliesByFeedbackIds(Collection<Long> feedbackIds) {
        if (feedbackIds == null || feedbackIds.isEmpty()) return Collections.emptyMap();
        var list = replyRepo.findByFeedback_FeedbackIdIn(feedbackIds);

        return list.stream().collect(Collectors.groupingBy(
                fr -> fr.getFeedback().getFeedbackId(),
                LinkedHashMap::new,
                Collectors.toList()
        ));
    }
}
