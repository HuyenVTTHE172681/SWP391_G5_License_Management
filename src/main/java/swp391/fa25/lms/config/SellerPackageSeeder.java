package swp391.fa25.lms.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import swp391.fa25.lms.model.SellerPackage;
import swp391.fa25.lms.repository.SellerPackageRepository;

import java.util.Arrays;
import java.util.List;

@Component
public class SellerPackageSeeder {
    @Autowired
    private SellerPackageRepository packageRepo;

    @PostConstruct
    public void initData(){
        if (packageRepo.count() == 0) {
            List<SellerPackage> packages = Arrays.asList(
                    createPackage("Gói 1 tháng", 1, 99000, "Thời hạn 1 tháng, dành cho người mới bắt đầu"),
                    createPackage("Gói 3 tháng", 3, 249000, "Tiết kiệm hơn 15% so với 1 tháng"),
                    createPackage("Gói 6 tháng", 6, 459000, "Tiết kiệm hơn 25%, phù hợp người bán ổn định"),
                    createPackage("Gói 12 tháng", 12, 799000, "Tiết kiệm gần 40%, dành cho người bán chuyên nghiệp")
            );

            packageRepo.saveAll(packages);
            System.out.println("✅ Đã khởi tạo 4 gói Seller mặc định vào database!");
        }
    }

    private SellerPackage createPackage(String name, int months, double price, String desc) {
        SellerPackage pkg = new SellerPackage();
        pkg.setPackageName(name);
        pkg.setDurationInMonths(months);
        pkg.setPrice(price);
        pkg.setDescription(desc);
        return pkg;
    }
}
