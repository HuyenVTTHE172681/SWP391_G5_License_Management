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
        if (roleRepository.count() == 0) {
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
            interaction.setIcon("fas fa-layer-group");

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

        // ============ TOOLS ============
        if (toolRepo.count() == 0) {
            List<Category> categories = categoryRepo.findAll();
            Account seller1 = accountRepo.findByEmail("seller1@example.com")
                    .orElseThrow(() -> new RuntimeException("Seller 1 not found"));
            Account seller2 = accountRepo.findByEmail("seller2@example.com")
                    .orElseThrow(() -> new RuntimeException("Seller 2 not found"));

            Tool t1 = new Tool();
            t1.setToolName("Email Bulk Sender Pro");
            t1.setDescription("Gửi email hàng loạt nhanh chóng, chống spam, tối ưu cho marketing.");
            t1.setImage("/images/tools/cafe.png");
            t1.setSeller(seller1);
            t1.setCategory(categories.get(0)); // Email
            t1.setStatus(Tool.Status.PUBLISHED);
            t1.setLoginMethod(Tool.LoginMethod.USER_PASSWORD);
            t1.setQuantity(10);
            t1.setCreatedAt(LocalDateTime.now().minusDays(5));

            Tool t2 = new Tool();
            t2.setToolName("Auto Like & Share Facebook");
            t2.setDescription("Tăng tương tác bài viết tự động, giúp quảng bá hiệu quả.");
            t2.setImage("/images/tools/copy_high_image.jpg");
            t2.setSeller(seller2);
            t2.setCategory(categories.get(2)); // Tăng tương tác
            t2.setStatus(Tool.Status.PUBLISHED);
            t2.setLoginMethod(Tool.LoginMethod.USER_PASSWORD);
            t2.setQuantity(10);
            t2.setCreatedAt(LocalDateTime.now().minusDays(7));

            Tool t3 = new Tool();
            t3.setToolName("SEO Keyword Analyzer");
            t3.setDescription("Phân tích từ khóa, hỗ trợ SEO toàn diện.");
            t3.setImage("/images/tools/hinh-shin-de-thuong-1.jpg");
            t3.setSeller(seller1);
            t3.setCategory(categories.get(3)); // SEO
            t3.setStatus(Tool.Status.PUBLISHED);
            t3.setLoginMethod(Tool.LoginMethod.USER_PASSWORD);
            t3.setQuantity(10);
            t3.setCreatedAt(LocalDateTime.now().minusDays(2));

            Tool t4 = new Tool();
            t4.setToolName("SEO Keyword Analyzer");
            t4.setDescription("Phân tích từ khóa, hỗ trợ SEO toàn diện.");
            t4.setImage("/images/tools/seo1.png");
            t4.setSeller(seller1);
            t4.setCategory(categories.get(3)); // SEO
            t4.setStatus(Tool.Status.PUBLISHED);
            t4.setLoginMethod(Tool.LoginMethod.USER_PASSWORD);
            t4.setQuantity(0);
            t4.setCreatedAt(LocalDateTime.now().minusDays(2));

            Tool t5 = new Tool();
            t5.setToolName("SEO Keyword Analyzer");
            t5.setDescription("Phân tích từ khóa, hỗ trợ SEO toàn diện.");
            t5.setImage("/images/tools/seo1.png");
            t5.setSeller(seller1);
            t5.setCategory(categories.get(3)); // SEO
            t5.setStatus(Tool.Status.PUBLISHED);
            t5.setLoginMethod(Tool.LoginMethod.USER_PASSWORD);
            t5.setQuantity(10);
            t5.setCreatedAt(LocalDateTime.now().minusDays(2));

            Tool t6 = new Tool();
            t6.setToolName("SEO Keyword Analyzer");
            t6.setDescription("Phân tích từ khóa, hỗ trợ SEO toàn diện.");
            t6.setImage("/images/tools/seo1.png");
            t6.setSeller(seller1);
            t6.setCategory(categories.get(3)); // SEO
            t6.setStatus(Tool.Status.PUBLISHED);
            t6.setLoginMethod(Tool.LoginMethod.USER_PASSWORD);
            t6.setQuantity(1);
            t6.setCreatedAt(LocalDateTime.now().minusDays(2));

            Tool t7 = new Tool();
            t7.setToolName("SEO Keyword Analyzer");
            t7.setDescription("Phân tích từ khóa, hỗ trợ SEO toàn diện.");
            t7.setImage("/images/tools/seo1.png");
            t7.setSeller(seller1);
            t7.setCategory(categories.get(3)); // SEO
            t7.setStatus(Tool.Status.PUBLISHED);
            t7.setLoginMethod(Tool.LoginMethod.USER_PASSWORD);
            t7.setQuantity(5);
            t7.setCreatedAt(LocalDateTime.now().minusDays(2));

            Tool t8 = new Tool();
            t8.setToolName("SEO Keyword Analyzer");
            t8.setDescription("Phân tích từ khóa, hỗ trợ SEO toàn diện.");
            t8.setImage("/images/tools/seo1.png");
            t8.setSeller(seller1);
            t8.setCategory(categories.get(3)); // SEO
            t8.setStatus(Tool.Status.PUBLISHED);
            t8.setLoginMethod(Tool.LoginMethod.USER_PASSWORD);
            t8.setQuantity(0);
            t8.setCreatedAt(LocalDateTime.now().minusDays(2));

            Tool t9 = new Tool();
            t9.setToolName("SEO Keyword Analyzer");
            t9.setDescription("Phân tích từ khóa, hỗ trợ SEO toàn diện.");
            t9.setImage("/images/tools/seo1.png");
            t9.setSeller(seller1);
            t9.setCategory(categories.get(3)); // SEO
            t9.setStatus(Tool.Status.PUBLISHED);
            t9.setLoginMethod(Tool.LoginMethod.USER_PASSWORD);
            t9.setQuantity(0);
            t9.setCreatedAt(LocalDateTime.now().minusDays(2));

            Tool t10 = new Tool();
            t10.setToolName("SEO Keyword Analyzer");
            t10.setDescription("Phân tích từ khóa, hỗ trợ SEO toàn diện.");
            t10.setImage("/images/tools/seo1.png");
            t10.setSeller(seller1);
            t10.setCategory(categories.get(3)); // SEO
            t10.setStatus(Tool.Status.PUBLISHED);
            t10.setLoginMethod(Tool.LoginMethod.USER_PASSWORD);
            t10.setQuantity(0);
            t10.setCreatedAt(LocalDateTime.now().minusDays(2));

            // === TOOL VỚI LOGIN METHOD = TOKEN ===
            Tool tokenTool = new Tool();
            tokenTool.setToolName("Facebook Token Generator");
            tokenTool.setDescription("Tool tạo token đăng nhập Facebook tự động, bảo mật cao.");
            tokenTool.setImage("/images/tools/token_tool.png");
            tokenTool.setSeller(seller1);
            tokenTool.setCategory(categories.get(2)); // Tăng tương tác
            tokenTool.setStatus(Tool.Status.PUBLISHED);
            tokenTool.setLoginMethod(Tool.LoginMethod.TOKEN);
            tokenTool.setQuantity(5);
            tokenTool.setCreatedAt(LocalDateTime.now().minusDays(3));

            Tool userPassTool = new Tool();
            userPassTool.setToolName("Instagram Auto Poster");
            userPassTool.setDescription("Tự động đăng bài Instagram, hỗ trợ nhiều tài khoản.");
            userPassTool.setImage("/images/tools/insta_poster.png");
            userPassTool.setSeller(seller2);
            userPassTool.setCategory(categories.get(1)); // Phần mềm
            userPassTool.setStatus(Tool.Status.PUBLISHED);
            userPassTool.setLoginMethod(Tool.LoginMethod.USER_PASSWORD);
            userPassTool.setQuantity(10);
            userPassTool.setCreatedAt(LocalDateTime.now().minusDays(1));

            toolRepo.saveAll(Arrays.asList(
                    t1, t2, t3, t4, t5, t6, t7, t8, t9, t10,
                    tokenTool, userPassTool
            ));
        } else {
            System.out.println("Tool already exist, skipping initialization.");
        }

        // Lấy lại seller & tool để dùng cho phần dưới
        List<Category> categories = categoryRepo.findAll();
        Account seller1 = accountRepo.findByEmail("seller1@example.com")
                .orElseThrow(() -> new RuntimeException("Seller 1 not found"));
        Account seller2 = accountRepo.findByEmail("seller2@example.com")
                .orElseThrow(() -> new RuntimeException("Seller 2 not found"));

        // ============ TOOL FILE ============
        Tool tokenTool = toolRepo.findByToolName("Facebook Token Generator")
                .orElseThrow(() -> new RuntimeException("Token Tool not found"));
        Tool userPassTool = toolRepo.findByToolName("Instagram Auto Poster")
                .orElseThrow(() -> new RuntimeException("UserPass Tool not found"));

        ToolFile tokenOriginal = new ToolFile();
        tokenOriginal.setTool(tokenTool);
        tokenOriginal.setFilePath("/files/facebook_token/original.zip");
        tokenOriginal.setFileType(ToolFile.FileType.ORIGINAL);
        tokenOriginal.setUploadedBy(seller1);
        tokenOriginal.setCreatedAt(LocalDateTime.now().minusDays(5));

        ToolFile tokenWrapped = new ToolFile();
        tokenWrapped.setTool(tokenTool);
        tokenWrapped.setFilePath("/files/facebook_token/wrapped.zip");
        tokenWrapped.setFileType(ToolFile.FileType.WRAPPED);
        tokenWrapped.setUploadedBy(seller1);
        tokenWrapped.setCreatedAt(LocalDateTime.now().minusDays(3));

        ToolFile instaOriginal = new ToolFile();
        instaOriginal.setTool(userPassTool);
        instaOriginal.setFilePath("/files/insta_poster/original.zip");
        instaOriginal.setFileType(ToolFile.FileType.ORIGINAL);
        instaOriginal.setUploadedBy(seller2);
        instaOriginal.setCreatedAt(LocalDateTime.now().minusDays(2));

        ToolFile instaWrapped = new ToolFile();
        instaWrapped.setTool(userPassTool);
        instaWrapped.setFilePath("/files/insta_poster/wrapped.zip");
        instaWrapped.setFileType(ToolFile.FileType.WRAPPED);
        instaWrapped.setUploadedBy(seller2);
        instaWrapped.setCreatedAt(LocalDateTime.now().minusDays(1));

        toolFileRepository.saveAll(Arrays.asList(tokenOriginal, tokenWrapped, instaOriginal, instaWrapped));
        System.out.println("Tool files initialized.");

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

            // Tool 1
            License l1 = new License("Gói dùng thử 7 ngày", tools.get(0), 7, 0.0, null, LocalDateTime.now().minusDays(3));
            License l2 = new License("Gói 1 tháng", tools.get(0), 30, 10000.0, null, LocalDateTime.now().minusDays(2));
            License l3 = new License("Gói 6 tháng", tools.get(0), 180, 12000.0, null, LocalDateTime.now().minusDays(1));

            // Tool 2
            License l4 = new License("Gói cơ bản", tools.get(1), 30, 10000.0, null, LocalDateTime.now().minusDays(3));
            License l5 = new License("Gói nâng cao", tools.get(1), 180, 16000.0, null, LocalDateTime.now().minusDays(2));
            License l6 = new License("Gói trọn đời", tools.get(1), null, 99000.0, null, LocalDateTime.now().minusDays(1));

            // Tool 3
            License l7 = new License("Gói dùng thử 14 ngày", tools.get(2), 14, 0.0, null, LocalDateTime.now().minusDays(2));
            License l8 = new License("Gói 1 năm", tools.get(2), 365, 150000.0, null, LocalDateTime.now().minusDays(1));

            // Tool 4
            License l9 = new License("Gói cơ bản", tools.get(3), 30, 30000.0, null, LocalDateTime.now().minusDays(2));
            License l10 = new License("Gói trọn đời", tools.get(3), null, 999000.0, null, LocalDateTime.now().minusDays(1));

            // Tool 5
            License l11 = new License("Gói dùng thử", tools.get(4), 7, 0.0, null, LocalDateTime.now().minusDays(2));
            License l12 = new License("Gói cao cấp", tools.get(4), 365, 500000.0, null, LocalDateTime.now().minusDays(1));

            // Tool 6
            License l13 = new License("Gói Standard", tools.get(5), 30, 299000.0, null, LocalDateTime.now().minusDays(2));
            License l14 = new License("Gói Pro", tools.get(5), 180, 590000.0, null, LocalDateTime.now().minusDays(1));

            // Tool 7
            License l15 = new License("Gói cơ bản", tools.get(6), 30, 12000.0, null, LocalDateTime.now().minusDays(2));
            License l16 = new License("Gói trọn đời", tools.get(6), null, 300000.0, null, LocalDateTime.now().minusDays(1));

            // Tool 8
            License l17 = new License("Gói học sinh/sinh viên", tools.get(7), 90, 19000.0, null, LocalDateTime.now().minusDays(2));
            License l18 = new License("Gói doanh nghiệp", tools.get(7), 365, 1000000000.0, null, LocalDateTime.now().minusDays(1));

            // Tool 9
            License l19 = new License("Gói cơ bản", tools.get(8), 30, 15000.0, null, LocalDateTime.now().minusDays(2));
            License l20 = new License("Gói trọn đời", tools.get(8), null, 999000.0, null, LocalDateTime.now().minusDays(1));

            // Tool 10
            License l21 = new License("Gói Premium", tools.get(9), 365, 149000.0, null, LocalDateTime.now().minusDays(1));
            License l22 = new License("Gói dùng thử", tools.get(9), 14, 0.0, null, LocalDateTime.now().minusDays(3));

            // License cho tokenTool
            License tokenLicense1 = new License("Gói dùng thử 7 ngày", tokenTool, 7, 10000.0, null, LocalDateTime.now().minusDays(2));
            License tokenLicense2 = new License("Gói 1 tháng", tokenTool, 30, 40000.0, null, LocalDateTime.now().minusDays(1));

            // License cho userPassTool
            License userPassLicense1 = new License("Gói 1 tháng", userPassTool, 30, 19000.0, null, LocalDateTime.now().minusDays(2));
            License userPassLicense2 = new License("Gói 6 tháng", userPassTool, 180, 69000.0, null, LocalDateTime.now().minusDays(1));

            licenseRepo.saveAll(Arrays.asList(
                    l1, l2, l3, l4, l5, l6, l7, l8, l9, l10,
                    l11, l12, l13, l14, l15, l16, l17, l18, l19, l20, l21, l22,
                    tokenLicense1, tokenLicense2, userPassLicense1, userPassLicense2
            ));

            System.out.println("Default licenses have been initialized.");

            // ============ LICENSE ACCOUNT (TOKEN STOCK) ============
            if (licenseAccountRepository.count() == 0) {
                LicenseAccount token1 = new LicenseAccount();
                token1.setLicense(tokenLicense1);        // token 7 ngày
                token1.setUsername("N/A");
                token1.setPassword("N/A");
                token1.setToken("ABC123XYZ");
                token1.setStatus(LicenseAccount.Status.ACTIVE);
                token1.setUsed(false);                   // chưa bán cho ai
                // order = null, start/endDate = null (sẽ set khi khách mua)

                LicenseAccount token2 = new LicenseAccount();
                token2.setLicense(tokenLicense1);        // token 7 ngày
                token2.setUsername("N/A");
                token2.setPassword("N/A");
                token2.setToken("XYZ789QWE");
                token2.setStatus(LicenseAccount.Status.ACTIVE);
                token2.setUsed(false);

                LicenseAccount token3 = new LicenseAccount();
                token3.setLicense(tokenLicense2);        // token 1 tháng
                token3.setUsername("N/A");
                token3.setPassword("N/A");
                token3.setToken("TOKEN_MONTH_01");
                token3.setStatus(LicenseAccount.Status.ACTIVE);
                token3.setUsed(false);

                licenseAccountRepository.saveAll(Arrays.asList(token1, token2, token3));
                System.out.println("License accounts (pre-created TOKEN stock) have been initialized.");
            } else {
                System.out.println("License accounts already exist, skipping initialization.");
            }

        } else {
            System.out.println("Licenses already exist, skipping initialization.");
        }

        // ============ WALLET ============
        List<Account> allAccounts = accountRepo.findAll();
        for (Account acc : allAccounts) {
            boolean hasWallet = walletRepository.findByAccount(acc).isPresent();
            if (!hasWallet) {
                Wallet wallet = new Wallet();
                wallet.setAccount(acc);
                wallet.setBalance(java.math.BigDecimal.ZERO);
                wallet.setCurrency("VND");
                wallet.setUpdatedAt(LocalDateTime.now());
                walletRepository.save(wallet);
                acc.setWallet(wallet);
                accountRepo.save(acc);
                System.out.println("Created default wallet for account: " +
                        acc.getEmail() + " (Role: " + acc.getRole().getRoleName() + ")");
            }
        }

        System.out.println("Wallet initialization completed successfully.");
    }
}
