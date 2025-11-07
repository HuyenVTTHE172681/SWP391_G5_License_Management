package swp391.fa25.lms.service.manager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import swp391.fa25.lms.repository.CustomerOrderRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service("managerCustomerOrderService")
public class CustomerOrderService {

    @Autowired
    private CustomerOrderRepository customerOrderRepository;

    /** 💰 Doanh thu theo tháng */
    public Map<Integer, Double> getMonthlyRevenue() {
        return customerOrderRepository.getMonthlyRevenue().stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).intValue(),
                        row -> ((Number) row[1]).doubleValue()
                ));
    }

    /** 🥇 Top 5 tool doanh thu cao nhất */
    public List<Object[]> getTop5ToolsByRevenue() {
        return customerOrderRepository.getTop5ToolsByRevenue();
    }

    /** 🏆 Top 5 seller doanh thu cao nhất */
    public List<Object[]> getTop5SellersByRevenue() {
        return customerOrderRepository.getTop5SellersByRevenue();
    }
}
