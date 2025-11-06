package swp391.fa25.lms.service.seller;

import org.springframework.stereotype.Service;
import swp391.fa25.lms.model.Account;

import java.time.LocalDateTime;

@Service("sellerAccountService")
public class AccountService {

    public boolean isSellerActive(Account seller) {
        if (seller == null || seller.getSellerExpiryDate() == null) {
            return false;
        }
        return seller.getSellerExpiryDate().isAfter(LocalDateTime.now());
    }
}
