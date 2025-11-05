package swp391.fa25.lms.service.seller;

import org.springframework.stereotype.Service;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.CustomerOrder;
import swp391.fa25.lms.repository.CustomerOrderRepository;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
public class SellerOrderService {

    private final CustomerOrderRepository orderRepo;

    public SellerOrderService(CustomerOrderRepository orderRepo) {
        this.orderRepo = orderRepo;
    }

    public List<CustomerOrder> getFilteredOrders(
            Account seller,
            String keyword,
            String status,
            LocalDate from,
            LocalDate to,
            String sort
    ) {
        List<CustomerOrder> orders = orderRepo.findByToolSeller(seller);

        // 🔍 Lọc theo keyword
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.toLowerCase();
            orders = orders.stream()
                    .filter(o -> o.getTool().getToolName().toLowerCase().contains(kw)
                            || o.getAccount().getFullName().toLowerCase().contains(kw))
                    .toList();
        }

        // 🔍 Lọc theo trạng thái
        if (status != null && !status.isBlank()) {
            orders = orders.stream()
                    .filter(o -> o.getOrderStatus().name().equalsIgnoreCase(status))
                    .toList();
        }

        // 🔍 Lọc theo ngày tạo
        if (from != null) {
            orders = orders.stream()
                    .filter(o -> !o.getCreatedAt().toLocalDate().isBefore(from))
                    .toList();
        }
        if (to != null) {
            orders = orders.stream()
                    .filter(o -> !o.getCreatedAt().toLocalDate().isAfter(to))
                    .toList();
        }

        // 🔃 Sắp xếp
        Comparator<CustomerOrder> cmp = switch (sort) {
            case "oldest" -> Comparator.comparing(CustomerOrder::getCreatedAt);
            case "highest" -> Comparator.comparing(CustomerOrder::getPrice).reversed();
            case "lowest" -> Comparator.comparing(CustomerOrder::getPrice);
            default -> Comparator.comparing(CustomerOrder::getCreatedAt).reversed(); // newest
        };
        return orders.stream().sorted(cmp).toList();
    }

    public double calculateTotalRevenue(List<CustomerOrder> orders) {
        return orders.stream()
                .filter(o -> o.getOrderStatus() == CustomerOrder.OrderStatus.SUCCESS)
                .mapToDouble(CustomerOrder::getPrice)
                .sum();
    }

    public List<CustomerOrder> paginate(List<CustomerOrder> orders, int page, int size) {
        int totalOrders = orders.size();
        int start = (page - 1) * size;
        int end = Math.min(start + size, totalOrders);
        return orders.subList(Math.min(start, end), end);
    }
}
