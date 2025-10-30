package swp391.fa25.lms.service.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.Role.RoleName;
import swp391.fa25.lms.repository.AccountRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service("adminHomeService")
public class AdminHomeService {

    @Autowired
    AccountRepository accountRepository;

    // KPI tổng hợp (trả về map cho gọn)
    public Map<String, Long> kpis() {
        Map<String, Long> m = new HashMap<>();
        m.put("totalUsers", accountRepository.count());
        m.put("totalSellers", accountRepository.countByRole_RoleName(RoleName.SELLER));
        m.put("totalCustomers", accountRepository.countByRole_RoleName(RoleName.CUSTOMER));
        return m;
    }

    public List<Account> latestAccounts(int limit) {
        return accountRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit)).getContent();
    }

    public List<Account> deactivatedAccounts(int limit) {
        return accountRepository
                .findByStatusOrderByUpdatedAtDesc(Account.AccountStatus.DEACTIVATED, PageRequest.of(0, limit))
                .getContent();
    }
}
