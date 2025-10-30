package swp391.fa25.lms.service.seller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.SellerPackage;
import swp391.fa25.lms.model.SellerSubscription;
import swp391.fa25.lms.repository.AccountRepository;
import swp391.fa25.lms.repository.SellerPackageRepository;
import swp391.fa25.lms.repository.SellerSubscriptionRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class SellerService {
    @Autowired
    private SellerPackageRepository packageRepo;

    @Autowired
    private AccountRepository accountRepo;

    @Autowired
    private SellerSubscriptionRepository sellerSubs;

    public List<SellerPackage> getAllPackage(){
        return packageRepo.findAll();
    }

    public Account renewSeller(String email, int packageId) {
        Account account = accountRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));

        SellerPackage selectedPackage = packageRepo.findById(packageId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy gói"));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate;
        LocalDateTime endDate;

        // ✅ Nếu còn hạn → nối tiếp, nếu hết hạn → tính lại từ ngày hiện tại
        if (account.getSellerExpiryDate() != null && account.getSellerExpiryDate().isAfter(now)) {
            startDate = account.getSellerExpiryDate();
        } else {
            startDate = now;
        }

        endDate = startDate.plusMonths(selectedPackage.getDurationInMonths());

        // ✅ Cập nhật account
        account.setSellerPackage(selectedPackage);
        account.setSellerActive(true);
        account.setSellerExpiryDate(endDate);
        account.setStatus(Account.AccountStatus.ACTIVE); // đảm bảo seller được kích hoạt
        accountRepo.save(account);

        // ✅ Tạo lịch sử gia hạn
        SellerSubscription subscription = new SellerSubscription();
        subscription.setAccount(account);
        subscription.setSellerPackage(selectedPackage);
        subscription.setStartDate(startDate);
        subscription.setEndDate(endDate);
        subscription.setPriceAtPurchase(selectedPackage.getPrice());
        subscription.setActive(true);

        sellerSubs.save(subscription);

        System.out.printf(
                "✅ Seller %s gia hạn thêm %d tháng. Bắt đầu từ: %s, hết hạn: %s%n",
                email, selectedPackage.getDurationInMonths(), startDate, endDate
        );

        return account;
    }

    public List<SellerSubscription> getSubscriptionHistory(String email) {
        Account account = accountRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));
        return sellerSubs.findByAccountOrderByStartDateDesc(account);
    }
}
