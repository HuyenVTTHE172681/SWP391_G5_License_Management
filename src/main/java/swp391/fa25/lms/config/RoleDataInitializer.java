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

    @Autowired
    private RoleRepo roleRepo;
    @Autowired
    private CategoryRepo categoryRepo;
    @Autowired
    private AccountRepo accountRepo;
    @Autowired
    private ToolRepo toolRepo;
    @Autowired
    private FeedbackRepo feedbackRepo;
    @Autowired
    private FavoriteRepo favoriteRepo;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // ============ ROLE ============
        // Kiểm tra DB xem đã có role chưa
        if(roleRepo.count() == 0) {
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
            roleRepo.saveAll(Arrays.asList(guest, customer, seller, mod, manager, admin));

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
            Role sellerRole = roleRepo.findByRoleName(RoleName.SELLER).get();
            Role customerRole = roleRepo.findByRoleName(RoleName.CUSTOMER).get();

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

            accountRepo.saveAll(Arrays.asList(seller1, seller2, customer1));
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
            t1.setImage("/images/tools/email1.png");
            t1.setSeller(seller1);
            t1.setCategory(categories.get(0)); // Email
            t1.setStatus(Tool.Status.APPROVED);
            t1.setCreatedAt(LocalDateTime.now().minusDays(5));

            Tool t2 = new Tool();
            t2.setToolName("Auto Like & Share Facebook");
            t2.setDescription("Tăng tương tác bài viết tự động, giúp quảng bá hiệu quả.");
            t2.setImage("/images/tools/interaction1.png");
            t2.setSeller(seller2);
            t2.setCategory(categories.get(2)); // Tăng tương tác
            t2.setStatus(Tool.Status.APPROVED);
            t2.setCreatedAt(LocalDateTime.now().minusDays(7));

            Tool t3 = new Tool();
            t3.setToolName("SEO Keyword Analyzer");
            t3.setDescription("Phân tích từ khóa, hỗ trợ SEO toàn diện.");
            t3.setImage("/images/tools/seo1.png");
            t3.setSeller(seller1);
            t3.setCategory(categories.get(3)); // SEO
            t3.setStatus(Tool.Status.APPROVED);
            t3.setCreatedAt(LocalDateTime.now().minusDays(2));

            toolRepo.saveAll(Arrays.asList(t1, t2, t3));
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

            feedbackRepo.saveAll(Arrays.asList(f1, f2, f3));
        } else {
            System.out.println("Feedback already exist, skipping initialization.");
        }

        // ============ FAVORITE ============
        if (favoriteRepo.count() == 0) {
            Account customer = accountRepo.findByEmail("customer1@example.com")
                    .orElseThrow(() -> new RuntimeException("Customer 1 not found"));
            List<Tool> tools = toolRepo.findAll();

            Favorite fav1 = new Favorite();
            fav1.setAccount(customer);
            fav1.setTool(tools.get(0));

            Favorite fav2 = new Favorite();
            fav2.setAccount(customer);
            fav2.setTool(tools.get(1));

            favoriteRepo.saveAll(Arrays.asList(fav1, fav2));
        } else {
            System.out.println("Favorite already exist, skipping initialization.");
        }
    }


}
