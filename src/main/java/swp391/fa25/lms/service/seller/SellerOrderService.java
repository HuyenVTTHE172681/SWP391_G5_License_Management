package swp391.fa25.lms.service.seller;

import org.springframework.stereotype.Service;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.CustomerOrder;
import swp391.fa25.lms.repository.CustomerOrderRepository;

import java.time.LocalDate;
import java.util.ArrayList;
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
        List<CustomerOrder> result = new ArrayList<>(orders);

// ================== FILTER KEYWORD ==================
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.toLowerCase();
            List<CustomerOrder> temp = new ArrayList<>();

            for (CustomerOrder o : result) {
                String toolName = o.getTool().getToolName().toLowerCase();
                String buyerName = o.getAccount().getFullName().toLowerCase();

                if (toolName.contains(kw) || buyerName.contains(kw)) {
                    temp.add(o);
                }
            }
            result = temp;
        }
        if (status != null && !status.isBlank()) {
            List<CustomerOrder> temp = new ArrayList<>();
            for (CustomerOrder o : result) {
                if (o.getOrderStatus().name().equalsIgnoreCase(status)) {
                    temp.add(o);
                }
            }
            result = temp;
        }
        if (from != null) {
            List<CustomerOrder> temp = new ArrayList<>();

            for (CustomerOrder o : result) {
                LocalDate created = o.getCreatedAt().toLocalDate();

                if (!created.isBefore(from)) {
                    temp.add(o);
                }
            }

            result = temp;
        }

        if (to != null) {
            List<CustomerOrder> temp = new ArrayList<>();

            for (CustomerOrder o : result) {
                LocalDate created = o.getCreatedAt().toLocalDate();

                if (!created.isAfter(to)) {
                    temp.add(o);
                }
            }
            result = temp;
        }

        orders = result;
        Comparator<CustomerOrder> cmp = switch (sort) {
            case "oldest" -> Comparator.comparing(CustomerOrder::getCreatedAt);
            case "highest" -> Comparator.comparing(CustomerOrder::getPrice).reversed();
            case "lowest" -> Comparator.comparing(CustomerOrder::getPrice);
            default -> Comparator.comparing(CustomerOrder::getCreatedAt).reversed();
        };
        return orders.stream().sorted(cmp).toList();
    }

    public double calculateTotalRevenue(List<CustomerOrder> orders) {
        double total = 0;
        for (CustomerOrder order : orders) {
            if (order.getOrderStatus() == CustomerOrder.OrderStatus.SUCCESS) {
                total += order.getPrice();
            }
        }
        return total;
    }

    public List<CustomerOrder> paginate(List<CustomerOrder> orders, int page, int size) {
        int totalOrders = orders.size();
        int start = (page - 1) * size;
        int end = Math.min(start + size, totalOrders);
        return orders.subList(Math.min(start, end), end);
    }
}
