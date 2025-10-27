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

    public Account renewSeller(String email, int packageId){
        Account account = accountRepo.findByEmail(email).
                orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));

        SellerPackage selectedPackage = packageRepo.findById(packageId).
                orElseThrow(() -> new RuntimeException("Không tìm thấy gói"));

        LocalDateTime startDate = LocalDateTime.now();
        LocalDateTime endDate = startDate.plusMonths(selectedPackage.getDurationInMonths());
        account.setSellerPackage(selectedPackage);
        account.setSellerActive(true);
        account.setSellerExpiryDate(endDate);
        accountRepo.save(account);

        SellerSubscription subscription = new SellerSubscription();
        subscription.setAccount(account);
        subscription.setSellerPackage(selectedPackage);
        subscription.setStartDate(startDate);
        subscription.setEndDate(endDate);
        subscription.setPriceAtPurchase(selectedPackage.getPrice());
        subscription.setActive(true);

        sellerSubs.save(subscription);
        return account;
    }

    public List<SellerSubscription> getSubscriptionHistory(String email) {
        Account account = accountRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));
        return sellerSubs.findByAccountOrderByStartDateDesc(account);
    }
}
