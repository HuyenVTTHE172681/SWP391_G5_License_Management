package swp391.fa25.lms.service.customer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.CustomerOrder;
import swp391.fa25.lms.repository.CustomerOrderRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class OrderService {

    @Autowired
    private CustomerOrderRepository orderRepository;

    /**
     * Lấy danh sách đơn hàng của user kèm các bộ lọc và phân trang
     */
    public Page<CustomerOrder> getFilteredOrders(
            Account account,
            String keyword,
            String status,
            String dateRange,
            String priceRange,
            int page,
            int size,
            String sortField,
            String sortDir) {

        // Pageable để chứa thông tin page/size/sort (dùng cho tạo PageImpl cuối cùng)
        Sort sort = Sort.by(sortField == null ? "createdAt" : sortField);
        if ("desc".equalsIgnoreCase(sortDir)) sort = sort.descending();
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), sort);

        // Lấy toàn bộ order của user (từ repository)
        List<CustomerOrder> orders = orderRepository.findByAccount_AccountIdOrderByCreatedAtDesc(account.getAccountId());

        LocalDateTime now = LocalDateTime.now();

        List<CustomerOrder> filtered = orders.stream()
                // Search theo mã, tool, người bán, license
                .filter(o -> {
                    if (keyword == null || keyword.isEmpty()) return true;
                    String kw = keyword.toLowerCase();
                    boolean matchesTool = o.getTool() != null && o.getTool().getToolName() != null && o.getTool().getToolName().toLowerCase().contains(kw);
                    boolean matchesSeller = o.getTool() != null && o.getTool().getSeller() != null && o.getTool().getSeller().getFullName() != null && o.getTool().getSeller().getFullName().toLowerCase().contains(kw);
                    boolean matchesLicense = o.getLicense() != null && o.getLicense().getName() != null && o.getLicense().getName().toLowerCase().contains(kw);
                    boolean matchesOrderId = o.getOrderId() != null && o.getOrderId().toString().contains(kw);
                    return matchesTool || matchesSeller || matchesLicense || matchesOrderId;
                })
                // Filter theo trạng thái
                .filter(o -> {
                    if (status == null || status.isEmpty()) return true;
                    return o.getOrderStatus() != null && o.getOrderStatus().name().equalsIgnoreCase(status);
                })
                // Filter theo thời gian (dateRange = số ngày)
                .filter(o -> {
                    if (dateRange == null || dateRange.isEmpty()) return true;
                    try {
                        long days = Long.parseLong(dateRange);
                        return o.getCreatedAt() != null && o.getCreatedAt().isAfter(now.minusDays(days));
                    } catch (NumberFormatException ex) {
                        return true;
                    }
                })
                // Filter theo đơn giá
                .filter(o -> {
                    if (priceRange == null || priceRange.isEmpty()) return true;
                    double price = o.getPrice();
                    return switch (priceRange) {
                        case "0-100000" -> price < 100000;
                        case "100000-500000" -> price >= 100000 && price <= 500000;
                        case "500000-1000000" -> price >= 500000 && price <= 1000000;
                        case "1000000+" -> price > 1000000;
                        default -> true;
                    };
                })
                .toList();

        // Sort theo trường chỉ định (nếu cần custom)
        Comparator<CustomerOrder> comparator;
        switch (sortField) {
            case "price" -> comparator = Comparator.comparing(CustomerOrder::getPrice, Comparator.nullsLast(Double::compareTo));
            case "orderId" -> comparator = Comparator.comparing(CustomerOrder::getOrderId, Comparator.nullsLast(Long::compareTo));
            default -> comparator = Comparator.comparing(CustomerOrder::getCreatedAt, Comparator.nullsLast(LocalDateTime::compareTo));
        }
        if ("desc".equalsIgnoreCase(sortDir)) comparator = comparator.reversed();
        filtered = filtered.stream().sorted(comparator).toList();

        // Logic Feedback/Report (set cờ nếu cần)
        filtered.forEach(order -> {
            boolean canFeedbackOrReport = false;
            if (order.getOrderStatus() == CustomerOrder.OrderStatus.SUCCESS &&
                    order.getCreatedAt() != null &&
                    Duration.between(order.getCreatedAt(), now).toDays() <= 7) {
                canFeedbackOrReport = true;
            }
            order.setCanFeedbackOrReport(canFeedbackOrReport);
        });

        // Phân trang thủ công an toàn: kiểm tra start/end
        int start = (int) pageable.getOffset();
        int total = filtered.size();
        if (start >= total) {
            // trả về trang rỗng (ví dụ user vừa xóa item và page hiện tại vượt quá tổng page)
            return new PageImpl<>(List.of(), pageable, total);
        }
        int end = Math.min(start + pageable.getPageSize(), total);
        List<CustomerOrder> pageContent = filtered.subList(start, end);

        // IMPORTANT: dùng filtered.size() (không phải filtered. Size())
        return new PageImpl<>(pageContent, pageable, total);
    }
}
