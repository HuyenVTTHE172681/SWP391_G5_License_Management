package swp391.fa25.lms.controller.admin;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriUtils;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.Role;
import swp391.fa25.lms.model.Role.RoleName;
import swp391.fa25.lms.repository.AccountRepository;
import swp391.fa25.lms.repository.RoleRepository;
import swp391.fa25.lms.service.admin.AdminAccountService;
import swp391.fa25.lms.service.admin.AdminHomeService;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class DashBoardAdminController {

    private static final String FIXED_ADMIN_EMAIL = "admin@gmail.com";
    @Autowired
    @Qualifier("adminHomeService")
    private  AdminHomeService adminHomeService;
    @Autowired
    @Qualifier("adminAccountService")
    private AdminAccountService adminAccountService;
    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private RoleRepository roleRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public DashBoardAdminController(AdminHomeService adminHomeService,
                                    AdminAccountService adminAccountService) {
        this.adminHomeService = adminHomeService;
        this.adminAccountService = adminAccountService;
    }
    @InitBinder("acc")
    public void initBinderForAcc(WebDataBinder binder) {
        // Trim và convert "" -> null
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
        // Tránh tham số filter "status" trên URL/hidden input bị bind vào acc.status (enum)
        binder.setDisallowedFields("status");
    }

    @GetMapping({"", "/"})
    public String root() {
        return "redirect:/admin/adminhome";
    }

    private static final java.util.Set<String> ALLOWED_SORT_FIELDS = java.util.Set.of(
            "accountId", "email", "fullName", "createdAt", "updatedAt"
    );

    private Sort parseSort(String sortStr, String fallbackProp, Sort.Direction fallbackDir) {
        if (!org.springframework.util.StringUtils.hasText(sortStr)) {
            return Sort.by(fallbackDir, fallbackProp);
        }
        String[] parts = sortStr.split(",", 2);
        String prop = (parts.length > 0 && org.springframework.util.StringUtils.hasText(parts[0])) ? parts[0].trim() : fallbackProp;
        if (!ALLOWED_SORT_FIELDS.contains(prop)) prop = fallbackProp;
        String dirStr = (parts.length > 1 && org.springframework.util.StringUtils.hasText(parts[1])) ? parts[1].trim() : fallbackDir.name();
        Sort.Direction dir;
        try {
            dir = Sort.Direction.fromString(dirStr);
        } catch (Exception e) {
            dir = fallbackDir;
        }
        return Sort.by(dir, prop);
    }

    // ===== Tab 1: HOME (KPI + 2 danh sách có search/sort/paging) =====
    @GetMapping("/adminhome")
    public String home(
            Model model,
            @ModelAttribute("msg") String msg,

            // Chỉ còn khối Latest registrations
            @RequestParam(name = "q1", defaultValue = "") String q1,
            @RequestParam(name = "sort1", defaultValue = "createdAt,desc") String sort1,
            @RequestParam(name = "p1", defaultValue = "0") int p1,
            @RequestParam(name = "s1", defaultValue = "5") int s1
    ) {
        // KPIs
        var kpis = adminHomeService.kpis();
        model.addAttribute("totalUsers", kpis.get("totalUsers"));
        model.addAttribute("totalSellers", kpis.get("totalSellers"));
        model.addAttribute("totalCustomers", kpis.get("totalCustomers"));

        // Parse sort cho Latest
        Sort sortLatest = parseSort(sort1, "createdAt", Sort.Direction.DESC);
        Pageable pageableLatest = PageRequest.of(Math.max(p1, 0), Math.max(s1, 1), sortLatest);

        // Latest registrations: không filter status (null)
        Page<Account> accLatestPage = adminAccountService.search(q1, pageableLatest, null);

        // Gắn model
        model.addAttribute("accLatestPage", accLatestPage);
        model.addAttribute("q1", q1);
        model.addAttribute("sort1", sort1);
        model.addAttribute("p1", p1);
        model.addAttribute("s1", s1);

        model.addAttribute("fixedAdminEmail", FIXED_ADMIN_EMAIL);
        model.addAttribute("page", "home");

        if (org.springframework.util.StringUtils.hasText(msg)) {
            model.addAttribute("msg", msg);
        }
        return "admin/adminhome";
    }

    // ===== Tab 2: ACCOUNTS (CRUD + filter/search) =====
    @GetMapping("/accounts")
    public String list(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String status, // đổi: mặc định All
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "accountId,asc") String sort,
            Model model,
            @ModelAttribute("msg") String msg) {

        String[] s = sort.split(",", 2);
        Sort.Direction dir = (s.length > 1 ? Sort.Direction.fromString(s[1]) : Sort.Direction.ASC);
        String prop = s.length > 0 ? s[0] : "accountId";
        Pageable pageable = PageRequest.of(page, size, Sort.by(dir, prop));

        Account.AccountStatus st = null;
        try {
            st = (status == null || status.isBlank()) ? null : Account.AccountStatus.valueOf(status);
        } catch (Exception ignored) {
        }

        Page<Account> accPage = adminAccountService.search(q, pageable, st);

        model.addAttribute("accPage", accPage);
        model.addAttribute("q", q);
        model.addAttribute("status", status);
        model.addAttribute("sort", sort);
        model.addAttribute("roles", RoleName.values());
        model.addAttribute("fixedAdminEmail", FIXED_ADMIN_EMAIL);
        model.addAttribute("page", "accounts");
        if (org.springframework.util.StringUtils.hasText(msg)) {
            model.addAttribute("msg", msg);
        }
        return "admin/accounts";
    }

    // GET: chi tiết
    @GetMapping("/accounts/{id}")
    public String detail(@PathVariable long id,
                         @RequestParam(defaultValue="") String q,
                         @RequestParam(defaultValue="") String status,
                         @RequestParam(defaultValue="0") int page,
                         @RequestParam(defaultValue="10") int size,
                         @RequestParam(defaultValue="accountId,asc") String sort,
                         @RequestParam(required=false) Integer edit,
                         Model model, RedirectAttributes ra) {
        var acc = adminAccountService.get(id);
        model.addAttribute("acc", acc);
        model.addAttribute("roles", RoleName.values());
        model.addAttribute("fixedAdminEmail", FIXED_ADMIN_EMAIL);
        model.addAttribute("q", q); model.addAttribute("status", status);
        model.addAttribute("page", page); model.addAttribute("size", size);
        model.addAttribute("sort", sort);
        model.addAttribute("edit", edit != null);
        return "admin/account-details"; // KHÔNG redirect ở đây
    }

    // POST: cập nhật
    @PostMapping("/accounts/{id}/update")
    public String update(@PathVariable long id,
                         @Valid @ModelAttribute("acc") Account acc,
                         BindingResult br,
                         @RequestParam(defaultValue="") String q,
                         @RequestParam(defaultValue="") String status,
                         @RequestParam(defaultValue="0") int page,
                         @RequestParam(defaultValue="10") int size,
                         @RequestParam(defaultValue="accountId,asc") String sort,
                         Model model, RedirectAttributes ra) {

        if (br.hasErrors()) {
            model.addAttribute("roles", RoleName.values());
            model.addAttribute("fixedAdminEmail", FIXED_ADMIN_EMAIL);
            model.addAttribute("q", q); model.addAttribute("status", status);
            model.addAttribute("page", page); model.addAttribute("size", size);
            model.addAttribute("sort", sort);
            model.addAttribute("edit", true);
            return "admin/account-details";
        }

        adminAccountService.updateBasicInfo(
                id,
                acc.getFullName(),
                (acc.getPhone()!=null && !acc.getPhone().isBlank()) ? acc.getPhone().trim() : null,
                acc.getAddress()
        );

        ra.addFlashAttribute("msg", "Updated profile");

        // ✅ để Spring tự encode mọi thứ
        ra.addAttribute("id", id);
        ra.addAttribute("q", q);
        ra.addAttribute("status", status);
        ra.addAttribute("page", page);
        ra.addAttribute("size", size);
        ra.addAttribute("sort", sort);
        return "redirect:/admin/accounts/{id}";
    }

    @PostMapping("/accounts/{id}/role")
    public String changeRole(@PathVariable long id,
                             @RequestParam("role") RoleName newRole,
                             @RequestParam(defaultValue = "list") String _return,
                             @RequestParam(defaultValue = "") String q,
                             @RequestParam(defaultValue = "") String status,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "10") int size,
                             @RequestParam(defaultValue = "accountId,asc") String sort,
                             RedirectAttributes ra) {
        try {
            adminAccountService.changeRole(id, newRole);
            ra.addFlashAttribute("msg", "Updated role to " + newRole + " for account " + id);
        } catch (Exception ex) {
            ra.addFlashAttribute("msg", ex.getMessage());
        }
        if ("detail".equalsIgnoreCase(_return)) {
            return "redirect:/admin/accounts/" + id
                    + "?q=" + UriUtils.encode(q, StandardCharsets.UTF_8)
                    + "&status=" + UriUtils.encode(status, StandardCharsets.UTF_8)
                    + "&page=" + page + "&size=" + size
                    + "&sort=" + UriUtils.encode(sort, StandardCharsets.UTF_8);
        }
        return "redirect:/admin/accounts?q=" + UriUtils.encode(q, StandardCharsets.UTF_8)
                + "&status=" + UriUtils.encode(status, StandardCharsets.UTF_8)
                + "&page=" + page + "&size=" + size
                + "&sort=" + UriUtils.encode(sort, StandardCharsets.UTF_8);
    }

    @PostMapping("/accounts/{id}/delete")
    public String delete(@PathVariable long id,
                         @RequestParam(defaultValue = "list") String _return,
                         @RequestParam(defaultValue = "") String q,
                         @RequestParam(defaultValue = "") String status,
                         @RequestParam(defaultValue = "0") int page,
                         @RequestParam(defaultValue = "10") int size,
                         @RequestParam(defaultValue = "accountId,asc") String sort,
                         RedirectAttributes ra) {
        try {
            Account acc = adminAccountService.get(id);
            if (acc != null && FIXED_ADMIN_EMAIL.equalsIgnoreCase(acc.getEmail())) {
                ra.addFlashAttribute("msg", "Không thể vô hiệu hóa tài khoản ADMIN cố định.");
            } else {
                adminAccountService.deactivate(id);
                ra.addFlashAttribute("msg", "Đã chuyển tài khoản " + id + " sang DEACTIVATED.");
            }
        } catch (Exception ex) {
            ra.addFlashAttribute("msg", ex.getMessage());
        }
        if ("detail".equalsIgnoreCase(_return)) {
            return "redirect:/admin/accounts/" + id
                    + "?q=" + UriUtils.encode(q, StandardCharsets.UTF_8)
                    + "&status=" + UriUtils.encode(status, StandardCharsets.UTF_8)
                    + "&page=" + page + "&size=" + size
                    + "&sort=" + UriUtils.encode(sort, StandardCharsets.UTF_8);
        }
        return "redirect:/admin/accounts?q=" + UriUtils.encode(q, StandardCharsets.UTF_8)
                + "&status=" + UriUtils.encode(status, StandardCharsets.UTF_8)
                + "&page=" + page + "&size=" + size
                + "&sort=" + UriUtils.encode(sort, StandardCharsets.UTF_8);
    }

    @PostMapping("/accounts/{id}/reactivate")
    public String reactivate(@PathVariable long id,
                             @RequestParam(defaultValue = "list") String _return,
                             @RequestParam(defaultValue = "") String q,
                             @RequestParam(defaultValue = "") String status,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "10") int size,
                             @RequestParam(defaultValue = "accountId,asc") String sort,
                             RedirectAttributes ra) {
        try {
            adminAccountService.reactivate(id);
            ra.addFlashAttribute("msg", "Đã kích hoạt lại tài khoản " + id + ".");
        } catch (Exception ex) {
            ra.addFlashAttribute("msg", ex.getMessage());
        }
        if ("detail".equalsIgnoreCase(_return)) {
            return "redirect:/admin/accounts/" + id
                    + "?q=" + UriUtils.encode(q, StandardCharsets.UTF_8)
                    + "&status=" + UriUtils.encode(status, StandardCharsets.UTF_8)
                    + "&page=" + page + "&size=" + size
                    + "&sort=" + UriUtils.encode(sort, StandardCharsets.UTF_8);
        }
        return "redirect:/admin/accounts?q=" + UriUtils.encode(q, StandardCharsets.UTF_8)
                + "&status=" + UriUtils.encode(status, StandardCharsets.UTF_8)
                + "&page=" + page + "&size=" + size
                + "&sort=" + UriUtils.encode(sort, StandardCharsets.UTF_8);
    }

    @GetMapping("/accounts/create")
    public String showCreateAccountForm(Model model) {
        model.addAttribute("account", new Account());
        model.addAttribute("roles", Role.RoleName.values());
        return "admin/account-create";
    }

    @PostMapping("/accounts/create")
    public String createAccount(
            @RequestParam String fullName,
            @RequestParam String email,
            @RequestParam String phone,
            @RequestParam String address,
            @RequestParam String password,
            @RequestParam String role,
            Model model)
    {
        // ====== VALIDATIONS ======

        // Kiểm tra empty
        if (fullName.isEmpty() || email.isEmpty() || phone.isEmpty() ||
                address.isEmpty() || password.isEmpty()) {

            model.addAttribute("error", "Vui lòng nhập đầy đủ thông tin!");
            return "admin/account-create";
        }

        // Validate fullname
        if (fullName.matches(".*[@$!%*?&^#()_+=-].*")) {
            model.addAttribute("error", "Tên không được chứa ký tự đặc biệt!");
            return "admin/account-create";
        }

        // Validate email
        if (!email.contains("@")) {
            model.addAttribute("error", "Email không hợp lệ! Vui lòng nhập đúng định dạng Gmail.");
            return "admin/account-create";
        }

        // Validate phone
        if (!phone.matches("^0\\d{9}$")) {
            model.addAttribute("error", "Số điện thoại phải bắt đầu bằng 0 và đủ 10 số!");
            return "admin/account-create";
        }

        // Validate address
        if (address.matches(".*[@$!%*?&^#()_+=-].*")) {
            model.addAttribute("error", "Địa chỉ không được chứa ký tự đặc biệt!");
            return "admin/account-create";
        }

        // Validate password strength
        if (password.length() < 8) {
            model.addAttribute("error", "Mật khẩu phải dài ít nhất 8 ký tự!");
            return "admin/account-create";
        }
        if (!password.matches(".*[a-z].*")) {
            model.addAttribute("error", "Mật khẩu phải có ít nhất 1 chữ thường!");
            return "admin/account-create";
        }
        if (!password.matches(".*[A-Z].*")) {
            model.addAttribute("error", "Mật khẩu phải có ít nhất 1 chữ hoa!");
            return "admin/account-create";
        }
        if (!password.matches(".*\\d.*")) {
            model.addAttribute("error", "Mật khẩu phải có ít nhất 1 chữ số!");
            return "admin/account-create";
        }
        if (!password.matches(".*[@$!%*?&^#()_+=-].*")) {
            model.addAttribute("error", "Mật khẩu phải có ít nhất 1 ký tự đặc biệt!");
            return "admin/account-create";
        }
        if (password.contains(" ")) {
            model.addAttribute("error", "Mật khẩu không được chứa dấu cách!");
            return "admin/account-create";
        }

        // ====== CHECK EMAIL ĐÃ TỒN TẠI ======
        if (accountRepository.findByEmail(email).isPresent()) {
            model.addAttribute("error", "Email đã tồn tại trong hệ thống!");
            return "admin/account-create";
        }

        // ====== CREATE ACCOUNT ======
        Account newAcc = new Account();
        newAcc.setFullName(fullName);
        newAcc.setEmail(email);
        newAcc.setPhone(phone);
        newAcc.setAddress(address);
        newAcc.setPassword(passwordEncoder.encode(password)); // mã hóa mật khẩu
        newAcc.setCreatedAt(LocalDateTime.now());
        newAcc.setStatus(Account.AccountStatus.ACTIVE);
        newAcc.setVerified(true);

        // Gán Role
        Role roleObj = roleRepo.findByRoleName(Role.RoleName.valueOf(role))
                .orElseThrow(() -> new RuntimeException("Không tìm thấy role!"));
        newAcc.setRole(roleObj);
        accountRepository.save(newAcc);
        // ====== REDIRECT SAU KHI TẠO THÀNH CÔNG ======
        return "redirect:/admin/accounts?msg=created";
    }

}
