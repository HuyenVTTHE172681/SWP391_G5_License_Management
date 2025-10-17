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
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // ============ ROLE ============
        // Kiểm tra DB xem đã có role chưa
        if(roleRepository.count() == 0) {
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
        if (accountRepo.count() == 0)  {
            Role sellerRole = roleRepository.findByRoleName(RoleName.SELLER).get();
            Role customerRole = roleRepository.findByRoleName(RoleName.CUSTOMER).get();
            Role adminRole = roleRepository.findByRoleName(RoleName.ADMIN).get();
            Role modRole = roleRepository.findByRoleName(RoleName.MOD).get();

            Account seller1 = new Account();
            seller1.setEmail("seller1@example.com");
            seller1.setPassword(passwordEncoder.encode("123456"));
            seller1.setVerified(true);
            seller1.setFullName("Nguyễn Văn Seller");
            seller1.setStatus(Account.AccountStatus.ACTIVE);
            seller1.setCreatedAt(LocalDateTime.now().minusDays(30));
            seller1.setRole(sellerRole);

            Account seller2 = new Account();
            seller2.setEmail("seller2@example.com");
            seller2.setPassword(passwordEncoder.encode("123456"));
            seller2.setVerified(true);
            seller2.setFullName("Trần Thị Seller");
            seller2.setStatus(Account.AccountStatus.ACTIVE);
            seller2.setCreatedAt(LocalDateTime.now().minusDays(15));
            seller2.setRole(sellerRole);

            Account customer1 = new Account();
            customer1.setEmail("customer1@example.com");
            customer1.setPassword(passwordEncoder.encode("123456"));
            customer1.setVerified(true);
            customer1.setFullName("Phạm Minh Khách");
            customer1.setStatus(Account.AccountStatus.ACTIVE);
            customer1.setCreatedAt(LocalDateTime.now().minusDays(10));
            customer1.setRole(customerRole);

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

            accountRepo.saveAll(Arrays.asList(seller1, seller2, customer1, admin, mod1));
        } else {
            System.out.println("Account already exist, skipping initialization.");
        }

        // ============ TOOLS ============
        if (toolRepo.count() == 0) {
            List<Category> categories = categoryRepo.findAll();
            Account seller1 = accountRepo.findByEmail("seller1@example.com")
                    .orElseThrow(() -> new RuntimeException("Seller 1 not found"));
            Account seller2 = accountRepo.findByEmail("seller2@example.com")
                    .orElseThrow(() -> new RuntimeException("Seller 1 not found"));

            Tool t1 = new Tool();
            t1.setToolName("Email Bulk Sender Pro");
            t1.setDescription("Gửi email hàng loạt nhanh chóng, chống spam, tối ưu cho marketing.");
            t1.setImage("/images/tools/cafe.png");
            t1.setSeller(seller1);
            t1.setCategory(categories.get(0)); // Email
            t1.setStatus(Tool.Status.APPROVED);
            t1.setCreatedAt(LocalDateTime.now().minusDays(5));

            Tool t2 = new Tool();
            t2.setToolName("Auto Like & Share Facebook");
            t2.setDescription("Tăng tương tác bài viết tự động, giúp quảng bá hiệu quả.");
            t2.setImage("/images/tools/copy_high_image.jpg");
            t2.setSeller(seller2);
            t2.setCategory(categories.get(2)); // Tăng tương tác
            t2.setStatus(Tool.Status.APPROVED);
            t2.setCreatedAt(LocalDateTime.now().minusDays(7));

            Tool t3 = new Tool();
            t3.setToolName("SEO Keyword Analyzer");
            t3.setDescription("Phân tích từ khóa, hỗ trợ SEO toàn diện.");
            t3.setImage("/images/tools/hinh-shin-de-thuong-1.jpg");
            t3.setSeller(seller1);
            t3.setCategory(categories.get(3)); // SEO
            t3.setStatus(Tool.Status.APPROVED);
            t3.setCreatedAt(LocalDateTime.now().minusDays(2));

            Tool t4 = new Tool();
            t4.setToolName("SEO Keyword Analyzer");
            t4.setDescription("Phân tích từ khóa, hỗ trợ SEO toàn diện.");
            t4.setImage("/images/tools/seo1.png");
            t4.setSeller(seller1);
            t4.setCategory(categories.get(3)); // SEO
            t4.setStatus(Tool.Status.APPROVED);
            t4.setCreatedAt(LocalDateTime.now().minusDays(2));

            Tool t5 = new Tool();
            t5.setToolName("SEO Keyword Analyzer");
            t5.setDescription("Phân tích từ khóa, hỗ trợ SEO toàn diện.");
            t5.setImage("/images/tools/seo1.png");
            t5.setSeller(seller1);
            t5.setCategory(categories.get(3)); // SEO
            t5.setStatus(Tool.Status.APPROVED);
            t5.setCreatedAt(LocalDateTime.now().minusDays(2));

            Tool t6 = new Tool();
            t6.setToolName("SEO Keyword Analyzer");
            t6.setDescription("Phân tích từ khóa, hỗ trợ SEO toàn diện.");
            t6.setImage("/images/tools/seo1.png");
            t6.setSeller(seller1);
            t6.setCategory(categories.get(3)); // SEO
            t6.setStatus(Tool.Status.APPROVED);
            t6.setCreatedAt(LocalDateTime.now().minusDays(2));

            Tool t7 = new Tool();
            t7.setToolName("SEO Keyword Analyzer");
            t7.setDescription("Phân tích từ khóa, hỗ trợ SEO toàn diện.");
            t7.setImage("/images/tools/seo1.png");
            t7.setSeller(seller1);
            t7.setCategory(categories.get(3)); // SEO
            t7.setStatus(Tool.Status.APPROVED);
            t7.setCreatedAt(LocalDateTime.now().minusDays(2));

            Tool t8 = new Tool();
            t8.setToolName("SEO Keyword Analyzer");
            t8.setDescription("Phân tích từ khóa, hỗ trợ SEO toàn diện.");
            t8.setImage("/images/tools/seo1.png");
            t8.setSeller(seller1);
            t8.setCategory(categories.get(3)); // SEO
            t8.setStatus(Tool.Status.APPROVED);
            t8.setCreatedAt(LocalDateTime.now().minusDays(2));

            Tool t9 = new Tool();
            t9.setToolName("SEO Keyword Analyzer");
            t9.setDescription("Phân tích từ khóa, hỗ trợ SEO toàn diện.");
            t9.setImage("/images/tools/seo1.png");
            t9.setSeller(seller1);
            t9.setCategory(categories.get(3)); // SEO
            t9.setStatus(Tool.Status.APPROVED);
            t9.setCreatedAt(LocalDateTime.now().minusDays(2));

            Tool t10 = new Tool();
            t10.setToolName("SEO Keyword Analyzer");
            t10.setDescription("Phân tích từ khóa, hỗ trợ SEO toàn diện.");
            t10.setImage("/images/tools/seo1.png");
            t10.setSeller(seller1);
            t10.setCategory(categories.get(3)); // SEO
            t10.setStatus(Tool.Status.APPROVED);
            t10.setCreatedAt(LocalDateTime.now().minusDays(2));



            toolRepo.saveAll(Arrays.asList(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10));
        } else {
            System.out.println("Tool already exist, skipping initialization.");
        }

        // ============ FEEDBACK ============
        if (feedbackRepo.count() == 0) {
            Account customer = accountRepo.findByEmail("customer1@example.com")
                    .orElseThrow(() -> new RuntimeException("Customer 1 not found"));
            List<Tool> tools = toolRepo.findAll();

            Feedback f1 = new Feedback();
            f1.setAccount(customer);
            f1.setTool(tools.get(0));
            f1.setRating(5);
            f1.setComment("Rất hữu ích, dễ sử dụng!");
            f1.setCreatedAt(LocalDateTime.now().minusDays(2));

            Feedback f2 = new Feedback();
            f2.setAccount(customer);
            f2.setTool(tools.get(1));
            f2.setRating(4);
            f2.setComment("Tốt, nhưng cần thêm tính năng lọc.");
            f2.setCreatedAt(LocalDateTime.now().minusDays(1));

            Feedback f3 = new Feedback();
            f3.setAccount(customer);
            f3.setTool(tools.get(2));
            f3.setRating(5);
            f3.setComment("Phân tích rất chi tiết, đáng tiền!");
            f3.setCreatedAt(LocalDateTime.now());

            Feedback f4 = new Feedback();
            f4.setAccount(customer);
            f4.setTool(tools.get(0));
            f4.setRating(3);
            f4.setComment("Kho su dung");
            f4.setCreatedAt(LocalDateTime.now().minusDays(8));

            feedbackRepo.saveAll(Arrays.asList(f1, f2, f3, f4));
        } else {
            System.out.println("Feedback already exist, skipping initialization.");
        }

        // ============ FAVORITE ============
        if (favoriteRepository.count() == 0) {
            Account customer = accountRepo.findByEmail("customer1@example.com")
                    .orElseThrow(() -> new RuntimeException("Customer 1 not found"));
            List<Tool> tools = toolRepo.findAll();

            Favorite fav1 = new Favorite();
            fav1.setAccount(customer);
            fav1.setTool(tools.get(0));

            Favorite fav2 = new Favorite();
            fav2.setAccount(customer);
            fav2.setTool(tools.get(1));

            favoriteRepository.saveAll(Arrays.asList(fav1, fav2));
        } else {
            System.out.println("Favorite already exist, skipping initialization.");
        }

        // ============ LICENSE ============
        if (licenseRepo.count() == 0) {
            List<Tool> tools = toolRepo.findAll();

            License l1 = new License();
            l1.setName("Gói dùng thử 7 ngày");
            l1.setTool(tools.get(0)); // Tool Email Bulk Sender Pro
            l1.setDurationDays(7);
            l1.setPrice(0.0);
            l1.setCreatedAt(LocalDateTime.now().minusDays(3));

            License l2 = new License();
            l2.setName("Gói 1 tháng");
            l2.setTool(tools.get(0));
            l2.setDurationDays(30);
            l2.setPrice(9.99);
            l2.setCreatedAt(LocalDateTime.now().minusDays(2));

            License l3 = new License();
            l3.setName("Gói 6 tháng");
            l3.setTool(tools.get(0));
            l3.setDurationDays(180);
            l3.setPrice(49.99);
            l3.setCreatedAt(LocalDateTime.now().minusDays(1));

            License l4 = new License();
            l4.setName("Gói trọn đời");
            l4.setTool(tools.get(1)); // Tool Auto Like & Share Facebook
            l4.setDurationDays(null); // Không giới hạn
            l4.setPrice(99.99);
            l4.setCreatedAt(LocalDateTime.now().minusDays(1));

            License l5 = new License();
            l5.setName("Gói Premium 1 năm");
            l5.setTool(tools.get(2)); // SEO Keyword Analyzer
            l5.setDurationDays(365);
            l5.setPrice(79.99);
            l5.setCreatedAt(LocalDateTime.now().minusDays(1));

            licenseRepo.saveAll(Arrays.asList(l1, l2, l3, l4, l5));
            System.out.println("Default licenses have been initialized.");
        } else {
            System.out.println("Licenses already exist, skipping initialization.");
        }

    }


}