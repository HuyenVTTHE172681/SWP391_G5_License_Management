package swp391.fa25.lms.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import swp391.fa25.lms.model.*;
import swp391.fa25.lms.model.Role.RoleName;
import swp391.fa25.lms.repository.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Component
public class RoleDataInitializer implements CommandLineRunner {
    private static final String FIXED_ADMIN_EMAIL = "admin@gmail.com";

    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private CategoryRepository categoryRepo;
    @Autowired
    private AccountRepository accountRepo;
    @Autowired
    private ToolRepository toolRepo;
    @Autowired
    private FeedbackRepository feedbackRepo;
    @Autowired
    private FavoriteRepository favoriteRepository;
    @Autowired
    private LicenseToolRepository licenseRepo;
    @Autowired
    private WalletRepository walletRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private ToolFileRepository toolFileRepository;
    @Autowired
    private LicenseAccountRepository licenseAccountRepository;

    @Override
    public void run(String... args) throws Exception {
        // ============ ROLE ============
        // Kiểm tra DB xem đã có role chưa
        if (roleRepository.count() == 0) {
            // Tạo các role mặc định
            Role guest = new Role();
            guest.setRoleId(1);
            guest.setRoleName(RoleName.GUEST);
            guest.setNote("Khách vãng lai");

            Role customer = new Role();
            customer.setRoleId(2);
            customer.setRoleName(RoleName.CUSTOMER);
            customer.setNote("Khách hàng");

            Role seller = new Role();
            seller.setRoleId(3);
            seller.setRoleName(RoleName.SELLER);
            seller.setNote("Người bán");

            Role mod = new Role();
            mod.setRoleId(4);
            mod.setRoleName(RoleName.MOD);
            mod.setNote("Người kiểm duyệt");

            Role manager = new Role();
            manager.setRoleId(5);
            manager.setRoleName(RoleName.MANAGER);
            manager.setNote("Quản lý");

            Role admin = new Role();
            admin.setRoleId(6);
            admin.setRoleName(RoleName.ADMIN);
            admin.setNote("Quản trị viên");

            // Lưu tất cả vào DB
            roleRepository.saveAll(Arrays.asList(guest, customer, seller, mod, manager, admin));

            System.out.println("Default roles have been initialized.");
        } else {
            System.out.println("Roles already exist, skipping initialization.");
        }

        // ============ CATEGORY ============
        if (categoryRepo.count() == 0) {
            Category email = new Category();
            email.setCategoryName("Email");
            email.setDescription("Gmail, Yahoo, Hotmail,... và nhiều hơn thế nữa");
            email.setIcon("far fa-envelope");

            Category software = new Category();
            software.setCategoryName("Phần mềm");
            software.setDescription("Dịch vụ code tool, đồ họa, video,... và các dịch vụ liên quan");
            software.setIcon("fas fa-terminal");

            Category interaction = new Category();
            interaction.setCategoryName("Tăng tương tác");
            interaction.setDescription("Tăng like, share, comment,... cho sản phẩm của bạn");
            interaction.setIcon("as fa-layer-group");

            Category seo = new Category();
            seo.setCategoryName("SEO & Marketing");
            seo.setDescription("Tối ưu hóa công cụ tìm kiếm và chiến dịch quảng cáo");
            seo.setIcon("fas fa-user");

            categoryRepo.saveAll(Arrays.asList(email, software, interaction, seo));

            System.out.println("Default categories have been initialized.");
        } else {
            System.out.println("Categories already exist, skipping initialization.");
        }

        // ============ ACCOUNT ============
        if (accountRepo.count() == 0) {
            Role sellerRole = roleRepository.findByRoleName(RoleName.SELLER).get();
            Role customerRole = roleRepository.findByRoleName(RoleName.CUSTOMER).get();
            Role adminRole = roleRepository.findByRoleName(RoleName.ADMIN).get();
            Role modRole = roleRepository.findByRoleName(RoleName.MOD).get();
            Role managerRole = roleRepository.findByRoleName(RoleName.MANAGER).get();

            Account seller1 = new Account();
            seller1.setEmail("seller1@example.com");
            seller1.setPassword(passwordEncoder.encode("123456"));
            seller1.setVerified(true);
            seller1.setFullName("Nguyễn Văn Seller");
            seller1.setStatus(Account.AccountStatus.ACTIVE);
            seller1.setCreatedAt(LocalDateTime.now().minusDays(30));
            seller1.setRole(sellerRole);
            seller1.setSellerActive(true);
            seller1.setSellerExpiryDate(LocalDateTime.now().plusDays(2));

            Account seller2 = new Account();
            seller2.setEmail("seller2@example.com");
            seller2.setPassword(passwordEncoder.encode("123456"));
            seller2.setVerified(true);
            seller2.setFullName("Trần Thị Seller");
            seller2.setStatus(Account.AccountStatus.ACTIVE);
            seller2.setCreatedAt(LocalDateTime.now().minusDays(15));
            seller2.setRole(sellerRole);
            seller2.setSellerActive(true);
            seller2.setSellerExpiryDate(LocalDateTime.of(2025, 11, 2, 22, 55, 0));

            Account customer1 = new Account();
            customer1.setEmail("customer1@example.com");
            customer1.setPassword(passwordEncoder.encode("123456"));
            customer1.setVerified(true);
            customer1.setFullName("Phạm Minh Khách");
            customer1.setStatus(Account.AccountStatus.ACTIVE);
            customer1.setCreatedAt(LocalDateTime.now().minusDays(10));
            customer1.setRole(customerRole);

            Account customer4 = new Account();
            customer4.setEmail("customer4@example.com");
            customer4.setPassword(passwordEncoder.encode("123456"));
            customer4.setVerified(true);
            customer4.setFullName("Bùi Duy Ngọc");
            customer4.setStatus(Account.AccountStatus.ACTIVE);
            customer4.setCreatedAt(LocalDateTime.now().minusDays(10));
            customer4.setRole(customerRole);

            Account admin = new Account();
            admin.setEmail(FIXED_ADMIN_EMAIL);
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setFullName("System Administrator");
            admin.setStatus(Account.AccountStatus.ACTIVE);
            admin.setCreatedAt(LocalDateTime.now());
            admin.setRole(adminRole);
            admin.setVerified(true);

            Account mod1 = new Account();
            mod1.setEmail("moderator1@example.com");
            mod1.setPassword(passwordEncoder.encode("123456"));
            mod1.setVerified(true);
            mod1.setFullName("Moderator đây");
            mod1.setStatus(Account.AccountStatus.ACTIVE);
            mod1.setCreatedAt(LocalDateTime.now().minusDays(10));
            mod1.setRole(modRole);

            accountRepo.saveAll(Arrays.asList(seller1, seller2, customer1, admin, mod1, customer4));
        } else {
            System.out.println("Account already exist, skipping initialization.");
        }



    }

    }



