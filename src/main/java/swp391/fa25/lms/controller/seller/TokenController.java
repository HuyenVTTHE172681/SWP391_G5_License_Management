package swp391.fa25.lms.controller.seller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import swp391.fa25.lms.model.*;
import swp391.fa25.lms.repository.*;
import swp391.fa25.lms.service.seller.ToolService;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/seller/tokens")
public class TokenController {

    @Autowired
    private ToolRepository toolRepository;

    @Autowired
    private LicenseAccountRepository licenseAccountRepository;

    @Autowired
    private ToolService toolService;

    @Autowired
    private LicenseToolRepository licenseRepo;

    /** ============================================================
     * 🔹 1️⃣ Trang quản lý token cho tool TẠM (chưa lưu DB)
     * ============================================================ */
    @GetMapping("/manage")
    public String manageTokens(HttpServletRequest request, Model model) {
        HttpSession session = request.getSession();

        // Lấy tool tạm đang lưu trong session
        Tool tempTool = (Tool) session.getAttribute("tempTool");
        if (tempTool == null) {
            return "redirect:/seller/tools/add";
        }

        // Lấy danh sách token tạm trong session
        List<String> tempTokens = (List<String>) session.getAttribute("tempTokens");
        if (tempTokens == null) {
            tempTokens = new ArrayList<>();
            session.setAttribute("tempTokens", tempTokens);
        }

        model.addAttribute("tool", tempTool);
        model.addAttribute("tempTokens", tempTokens);
        return "seller/token-manage"; // Giao diện CRUD token
    }

    /** ============================================================
     * 🔹 2️⃣ Thêm 1 token đơn lẻ (lưu tạm vào session)
     * ============================================================ */
    @PostMapping("/add-single")
    public String addSingleToken(HttpServletRequest request,
                                 @RequestParam("token") String token,
                                 RedirectAttributes redirectAttributes) {

        HttpSession session = request.getSession();
        List<String> tempTokens = (List<String>) session.getAttribute("tempTokens");
        if (tempTokens == null) tempTokens = new ArrayList<>();

        token = token.trim();

        // ✅ Validate token: phải gồm đúng 6 chữ số
        if (!token.matches("^\\d{6}$")) {
            redirectAttributes.addFlashAttribute("error", "❌ Token phải gồm đúng 6 chữ số.");
            return "redirect:/seller/tokens/manage";
        }

        // ✅ Check trùng
        if (tempTokens.contains(token) || licenseAccountRepository.existsByToken(token)) {
            redirectAttributes.addFlashAttribute("error", "❌ Token đã tồn tại!");
            return "redirect:/seller/tokens/manage";
        }

        tempTokens.add(token);
        session.setAttribute("tempTokens", tempTokens);
        redirectAttributes.addFlashAttribute("success", "✅ Thêm token thành công!");
        return "redirect:/seller/tokens/manage";
    }

    /** ============================================================
     * 🔹 3️⃣ Thêm nhiều token cùng lúc (textarea)
     * ============================================================ */
    @PostMapping("/add-multiple")
    public String addMultipleTokens(@RequestParam("tokens") List<String> tokens,
                                    RedirectAttributes redirectAttributes,
                                    HttpSession session) {

        Tool currentTool = (Tool) session.getAttribute("tempTool");
        if (currentTool == null) {
            redirectAttributes.addFlashAttribute("error", "Phiên thêm tool đã hết hạn. Vui lòng tạo lại tool.");
            return "redirect:/seller/tools";
        }

        List<String> tempTokens = (List<String>) session.getAttribute("tempTokens");
        if (tempTokens == null) tempTokens = new ArrayList<>();

        int max = currentTool.getQuantity();

        // 🔹 validate từng token
        for (String t : tokens) {
            if (t == null || t.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "❌ Token không được để trống.");
                return "redirect:/seller/tokens/manage";
            }
            if (!t.matches("^\\d{6}$")) {
                redirectAttributes.addFlashAttribute("error", "❌ Token '" + t + "' phải gồm đúng 6 chữ số.");
                return "redirect:/seller/tokens/manage";
            }
            // ✅ Kiểm tra token đã tồn tại trong session chưa
            if (tempTokens.contains(t)) {
                redirectAttributes.addFlashAttribute("error", "❌ Token '" + t + "' đã tồn tại trong danh sách tạm.");
                return "redirect:/seller/tokens/manage";
            }

            // ✅ Kiểm tra token đã tồn tại trong DB chưa
            if (licenseAccountRepository.existsByToken(t)) {
                redirectAttributes.addFlashAttribute("error", "❌ Token '" + t + "' đã tồn tại trong hệ thống!");
                return "redirect:/seller/tokens/manage";
            }
        }

        // 🔹 Kiểm tra tổng số lượng không vượt quá
        if (tempTokens.size() + tokens.size() > max) {
            redirectAttributes.addFlashAttribute("error", "❌ Không thể thêm " + tokens.size() +
                    " token. Số lượng vượt quá giới hạn (" + max + ").");
            return "redirect:/seller/tokens/manage";
        }

        tempTokens.addAll(tokens);
        session.setAttribute("tempTokens", tempTokens);

        redirectAttributes.addFlashAttribute("success", "✅ Đã thêm " + tokens.size() + " token thành công!");
        return "redirect:/seller/tokens/manage";
    }

    /** ============================================================
     * 🔹 4️⃣ Xóa token tạm (trong session)
     * ============================================================ */
    @PostMapping("/delete")
    public String deleteToken(HttpServletRequest request,
                              @RequestParam("index") int index,
                              RedirectAttributes redirectAttributes) {

        HttpSession session = request.getSession();
        List<String> tempTokens = (List<String>) session.getAttribute("tempTokens");
        if (tempTokens != null && index >= 0 && index < tempTokens.size()) {
            tempTokens.remove(index);
            session.setAttribute("tempTokens", tempTokens);
            redirectAttributes.addFlashAttribute("success", "✅ Token removed!");
        } else {
            redirectAttributes.addFlashAttribute("error", "❌ Invalid token index!");
        }

        return "redirect:/seller/tokens/manage";
    }

    /** ============================================================
     * 🔹 5️⃣ Submit hoàn tất — tạo tool thật + lưu token vào DB
     * ============================================================ */
    @PostMapping("/finalize")
    @Transactional
    public String finalizeTool(HttpServletRequest request,
                               RedirectAttributes redirectAttributes) {

        HttpSession session = request.getSession();

        // ✅ Lấy dữ liệu tạm
        Tool tempTool = (Tool) session.getAttribute("tempTool");
        List<String> tempTokens = (List<String>) session.getAttribute("tempTokens");
        List<Integer> licenseDays = (List<Integer>) session.getAttribute("licenseDays");
        List<Double> licensePrices = (List<Double>) session.getAttribute("licensePrices");

        if (tempTool == null) {
            redirectAttributes.addFlashAttribute("error",
                    "⚠️ Phiên thêm tool đã hết hạn. Vui lòng tạo lại tool.");
            return "redirect:/seller/tools";
        }

        if (tempTokens == null || tempTokens.isEmpty()) {
            redirectAttributes.addFlashAttribute("error",
                    "❌ Bạn phải thêm ít nhất 1 token trước khi hoàn tất tạo tool.");
            return "redirect:/seller/tokens/manage";
        }
        System.out.println("=== [FINALIZE TOOL CREATION] ===");
        System.out.println("Tool Name: " + tempTool.getToolName());
        System.out.println("Tokens: " + tempTokens.size());
        System.out.println("Licenses: " + (licenseDays != null ? licenseDays.size() : 0));

        try {
            Account seller = (Account) session.getAttribute("loggedInAccount");

            // ✅ 1️⃣ Tạo tool thật trong DB
            tempTool.setSeller(seller);
            tempTool.setStatus(Tool.Status.PENDING);
            tempTool.setLoginMethod(Tool.LoginMethod.TOKEN);
            tempTool.setCreatedAt(LocalDateTime.now());
            tempTool.setUpdatedAt(LocalDateTime.now());
            Tool saved = toolService.addTool(tempTool, seller);

            System.out.println("✅ Tool saved to DB with ID: " + saved.getToolId());

            // ✅ 2️⃣ Thêm License (nếu có)
            if (licenseDays != null && licensePrices != null) {
                for (int i = 0; i < Math.min(licenseDays.size(), licensePrices.size()); i++) {
                    Integer days = licenseDays.get(i);
                    Double price = licensePrices.get(i);
                    if (days == null || price == null || days <= 0 || price < 0) continue;

                    License l = new License();
                    l.setTool(saved);
                    l.setDurationDays(days);
                    l.setPrice(price);
                    l.setName("License " + days + " days");
                    l.setCreatedAt(LocalDateTime.now());
                    licenseRepo.save(l);

                    System.out.println("   → License added: " + days + " days / " + price + "đ");
                }
            }

            // ✅ 3️⃣ Thêm Token vào DB
            for (String token : tempTokens) {
                if (token == null || token.isBlank()) continue;

                LicenseAccount acc = new LicenseAccount();
                acc.setTool(saved);
                acc.setLoginMethod(LicenseAccount.LoginMethod.TOKEN);
                acc.setToken(token.trim());
                acc.setUsed(false);
                acc.setStatus(LicenseAccount.Status.ACTIVE);
                acc.setOrder(null);
                acc.setStartDate(LocalDateTime.now());
                licenseAccountRepository.save(acc);

                System.out.println("   → Token inserted: " + token);
            }

            // ✅ 4️⃣ Xóa dữ liệu session tạm
            session.removeAttribute("tempTool");
            session.removeAttribute("tempTokens");
            session.removeAttribute("licenseDays");
            session.removeAttribute("licensePrices");
            System.out.println("🧹 Cleared session temp data.");

            // ✅ 5️⃣ Redirect về trang seller/tools
            redirectAttributes.addFlashAttribute("success",
                    "🎉 Tool '" + saved.getToolName() + "' created successfully with "
                            + tempTokens.size() + " tokens and "
                            + (licenseDays != null ? licenseDays.size() : 0) + " licenses!");
            System.out.println("=== [FINALIZE DONE] ===");
            return "redirect:/seller/tools";

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "❌ Error: " + e.getMessage());
            return "redirect:/seller/tools/add";
        }
    }


    /** ============================================================
     * 🔹 6️⃣ Nếu user bấm "Back" → quay lại form tạo tool
     * ============================================================ */
    @GetMapping("/back")
    public String backToToolAdd(HttpServletRequest request) {
        HttpSession session = request.getSession();
        Tool tempTool = (Tool) session.getAttribute("tempTool");
        if (tempTool == null) {
            return "redirect:/seller/tools/add";
        }
        return "redirect:/seller/tools/add"; // Dữ liệu tool vẫn còn vì đang giữ trong session
    }
}
