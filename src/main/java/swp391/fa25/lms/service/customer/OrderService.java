package swp391.fa25.lms.service.customer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.CustomerOrder;
import swp391.fa25.lms.repository.CustomerOrderRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService {

    @Autowired
    private CustomerOrderRepository orderRepository;

    /**
     * Lấy danh sách đơn hàng của user kèm logic xử lý hiển thị.
     */
    public Page<CustomerOrder> getFilteredOrders(Account account, String keyword, String status,
                                                 int page, int size, String sortField, String sortDir) {
        Sort sort = Sort.by(sortField);
        if ("desc".equalsIgnoreCase(sortDir)) sort = sort.descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        List<CustomerOrder> orders = orderRepository.findByAccount_AccountIdOrderByCreatedAtDesc(account.getAccountId());

        // Filter logic
        List<CustomerOrder> filtered = orders.stream()
                .filter(o -> keyword == null || keyword.isEmpty() ||
                        o.getTool().getToolName().toLowerCase().contains(keyword.toLowerCase()) ||
                        o.getTool().getSeller().getFullName().toLowerCase().contains(keyword.toLowerCase()) ||
                        o.getOrderId().toString().contains(keyword))
                .filter(o -> status == null || status.isEmpty() ||
                        o.getOrderStatus().name().equalsIgnoreCase(status))
                .toList();

        // Gắn logic feedback/report
        LocalDateTime now = LocalDateTime.now();
        filtered.forEach(order -> {
            boolean canFeedbackOrReport = false;
            if (order.getOrderStatus() == CustomerOrder.OrderStatus.SUCCESS &&
                    Duration.between(order.getCreatedAt(), now).toDays() <= 7) {
                canFeedbackOrReport = true;
            }
            order.setCanFeedbackOrReport(canFeedbackOrReport);
        });

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filtered.size());
        List<CustomerOrder> pageContent = filtered.subList(start, end);
        return new PageImpl<>(pageContent, pageable, filtered.size());
    }
}
