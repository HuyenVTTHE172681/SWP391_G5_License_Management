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

@Controller
@RequestMapping("/seller/tokens")
public class TokenController {

    @Autowired private ToolRepository toolRepository;
    @Autowired private LicenseAccountRepository licenseAccountRepository;
    @Autowired private LicenseToolRepository licenseRepo;
    @Autowired private ToolService toolService;
    private String redirectToManage(Long toolId, HttpServletRequest request) {
        HttpSession s = request.getSession(false);
        boolean inEdit = false;
        if (s != null) {
            Tool editTemp = (Tool) s.getAttribute("editToolTemp");
            inEdit = (editTemp != null && Objects.equals(editTemp.getToolId(), toolId));
        }
        return "redirect:/seller/tokens/manage/" + toolId + (inEdit ? "?mode=edit" : "");
    }
    @GetMapping(value = {"/manage", "/manage/{toolId}"})
    public String manageTokens(@PathVariable(required = false) Long toolId,
                               @RequestParam(value = "mode", required = false) String mode,
                               HttpServletRequest request,
                               Model model,
                               RedirectAttributes redirectAttributes) {

        HttpSession session = request.getSession();
        Account seller = (Account) session.getAttribute("loggedInAccount");

        // 🧩 SESSION MODE
        if (toolId == null) {
            Tool tempTool = (Tool) session.getAttribute("tempTool");
            if (tempTool == null) {
                redirectAttributes.addFlashAttribute("error", "⚠️ Phiên tạo tool đã hết hạn!");
                return "redirect:/seller/tools/add";
            }
            @SuppressWarnings("unchecked")
            List<String> tempTokens = (List<String>) session.getAttribute("tempTokens");
            if (tempTokens == null) tempTokens = new ArrayList<>();

            int max = tempTool.getQuantity() == null ? 0 : tempTool.getQuantity();
            int count = tempTokens.size();

            model.addAttribute("tool", tempTool);
            model.addAttribute("tokens", tempTokens);
            model.addAttribute("isTemp", true);
            model.addAttribute("count", count);
            model.addAttribute("max", max);
            model.addAttribute("remaining", Math.max(0, max - count));
            return "seller/token-manage";
        }

        // 💾 DB MODE
        Tool tool = toolRepository.findById(toolId).orElse(null);
        if (tool == null || seller == null || !tool.getSeller().getAccountId().equals(seller.getAccountId())) {
            redirectAttributes.addFlashAttribute("error", "❌ Tool không tồn tại hoặc bạn không có quyền truy cập.");
            return "redirect:/seller/tools";
        }

        List<LicenseAccount> tokens = licenseAccountRepository.findByToolToolId(toolId);
        boolean editMode = "edit".equalsIgnoreCase(mode);

        Tool editTemp = (Tool) session.getAttribute("editToolTemp");
        if (editTemp != null && Objects.equals(editTemp.getToolId(), toolId)) {
            editMode = true;
        }

        Integer displayQty = tool.getQuantity();
        if (editMode && editTemp != null && Objects.equals(editTemp.getToolId(), toolId)) {
            displayQty = editTemp.getQuantity();
        }

        if (editMode) {
            model.addAttribute("info", "⚙️ Edit Mode — Vui lòng chỉnh sửa token sao cho khớp với quantity mới.");
        }

        model.addAttribute("editMode", editMode);
        model.addAttribute("tool", tool);
        model.addAttribute("tokens", tokens);
        model.addAttribute("isTemp", false);
        model.addAttribute("count", tokens.size());
        model.addAttribute("max", displayQty == null ? 0 : displayQty);
        model.addAttribute("remaining", Math.max(0, (displayQty == null ? 0 : displayQty) - tokens.size()));
        return "seller/token-manage";
    }


    /** ============================================================
     * 🔹 2️⃣ Thêm token đơn lẻ
     * ============================================================ */
    @PostMapping("/add-single")
    @Transactional
    public String addSingleToken(@RequestParam("tokenKey") String tokenKey,
                                 @RequestParam(value = "toolId", required = false) Long toolId,
                                 HttpServletRequest request,
                                 RedirectAttributes redirectAttributes) {

        HttpSession session = request.getSession();
        tokenKey = tokenKey.trim();

        if (!tokenKey.matches("^\\d{6}$")) {
            redirectAttributes.addFlashAttribute("error", "❌ Token phải gồm đúng 6 chữ số.");
            return toolId == null ? "redirect:/seller/tokens/manage" : "redirect:/seller/tokens/manage/" + toolId;
        }

        // 🧩 SESSION MODE
        if (toolId == null) {
            @SuppressWarnings("unchecked")
            List<String> tempTokens = (List<String>) session.getAttribute("tempTokens");
            if (tempTokens == null) tempTokens = new ArrayList<>();

            Tool tempTool = (Tool) session.getAttribute("tempTool");
            int max = tempTool == null || tempTool.getQuantity() == null ? 0 : tempTool.getQuantity();

            if (tempTokens.size() >= max) {
                redirectAttributes.addFlashAttribute("error", "❌ Đã đủ số lượng (" + max + "). Không thể thêm nữa.");
                return "redirect:/seller/tokens/manage";
            }

            if (tempTokens.contains(tokenKey) || licenseAccountRepository.existsByToken(tokenKey)) {
                redirectAttributes.addFlashAttribute("error", "❌ Token đã tồn tại!");
                return "redirect:/seller/tokens/manage";
            }

            tempTokens.add(tokenKey);
            session.setAttribute("tempTokens", tempTokens);

            tempTokens.add(tokenKey);
            session.setAttribute("tempTokens", tempTokens);
            redirectAttributes.addFlashAttribute("success", "✅ Token thêm tạm thành công!");
            return "redirect:/seller/tokens/manage";
        }

        // 💾 DB MODE
        Account seller = (Account) session.getAttribute("loggedInAccount");
        Tool tool = toolRepository.findById(toolId).orElse(null);
        if (tool == null || !tool.getSeller().getAccountId().equals(seller.getAccountId())) {
            redirectAttributes.addFlashAttribute("error", "Không thể thêm token cho tool này.");
            return "redirect:/seller/tools";
        }

        int max = tool.getQuantity() == null ? 0 : tool.getQuantity();
        int current = licenseAccountRepository.findByToolToolId(toolId).size();
        if (current >= max) {
            if (current > max) {
                redirectAttributes.addFlashAttribute("error",
                        "⚠️ Số lượng token hiện tại (" + current + ") vượt quá quantity mới (" + max + "). "
                                + "Vui lòng xóa bớt token trước khi thêm mới.");
            } else {
                redirectAttributes.addFlashAttribute("error",
                        "❌ Đã đủ số lượng (" + max + "). Không thể thêm nữa.");
            }
            return "redirect:/seller/tokens/manage/" + toolId;
        }

        if (licenseAccountRepository.existsByToken(tokenKey)) {
            redirectAttributes.addFlashAttribute("error", "❌ Token đã tồn tại!");
            return "redirect:/seller/tokens/manage/" + toolId;
        }

        LicenseAccount acc = new LicenseAccount();
        acc.setTool(tool);
        acc.setLoginMethod(LicenseAccount.LoginMethod.TOKEN);
        acc.setToken(tokenKey);
        acc.setUsed(false);
        acc.setStatus(LicenseAccount.Status.ACTIVE);
        acc.setStartDate(LocalDateTime.now());
        licenseAccountRepository.save(acc);

        redirectAttributes.addFlashAttribute("success", "✅ Token thêm thành công!");
        return "redirect:/seller/tokens/manage/" + toolId;
    }

    /** ============================================================
     * 🔹 3️⃣ Thêm nhiều token
     * ============================================================ */
    @PostMapping("/add-multiple")
    @Transactional
    public String addMultipleTokens(@RequestParam("tokens") List<String> tokens,
                                    @RequestParam(value = "toolId", required = false) Long toolId,
                                    HttpServletRequest request,
                                    RedirectAttributes redirectAttributes) {

        HttpSession session = request.getSession();

        // 🧩 SESSION MODE
        if (toolId == null) {
            Tool tempTool = (Tool) session.getAttribute("tempTool");
            if (tempTool == null) {
                redirectAttributes.addFlashAttribute("error", "⚠️ Phiên tạo tool đã hết hạn.");
                return "redirect:/seller/tools/add";
            }

            @SuppressWarnings("unchecked")
            List<String> tempTokens = (List<String>) session.getAttribute("tempTokens");
            if (tempTokens == null) tempTokens = new ArrayList<>();

            int max = tempTool.getQuantity() == null ? 0 : tempTool.getQuantity();
            int remaining = max - tempTokens.size();
            if (remaining <= 0) {
                redirectAttributes.addFlashAttribute("error", "❌ Đã đủ số lượng (" + max + ").");
                return "redirect:/seller/tokens/manage";
            }

            List<String> clean = new ArrayList<>();
            for (String t : tokens) {
                if (t == null) continue;
                t = t.trim();
                if (!t.matches("^\\d{6}$")) {
                    redirectAttributes.addFlashAttribute("error", "❌ Token '" + t + "' không hợp lệ. Phải có đúng 6 số");
                    return "redirect:/seller/tokens/manage";
                }
                if (tempTokens.contains(t) || licenseAccountRepository.existsByToken(t)) {
                    redirectAttributes.addFlashAttribute("error", "❌ Token '" + t + "' đã tồn tại!");
                    return "redirect:/seller/tokens/manage";
                }
                clean.add(t);
            }

            if (clean.size() > remaining) {
                redirectAttributes.addFlashAttribute("error", "❌ Bạn chỉ có thể thêm tối đa " + remaining + " token nữa.");
                return "redirect:/seller/tokens/manage";
            }

            tempTokens.addAll(clean);
            session.setAttribute("tempTokens", tempTokens);
            redirectAttributes.addFlashAttribute("success", "✅ Đã thêm " + clean.size() + " token tạm!");
            return "redirect:/seller/tokens/manage";
        }
        // 💾 DB MODE
        Account seller = (Account) session.getAttribute("loggedInAccount");
        if (seller == null) {
            redirectAttributes.addFlashAttribute("error", "⚠️ Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
            return "redirect:/login";
        }
        Tool tool = toolRepository.findById(toolId).orElse(null);
        if (tool == null || !tool.getSeller().getAccountId().equals(seller.getAccountId())) {
            redirectAttributes.addFlashAttribute("error", "Không thể thêm token cho tool này.");
            return "redirect:/seller/tools";
        }
        int max = tool.getQuantity() == null ? 0 : tool.getQuantity();
        int current = licenseAccountRepository.findByToolToolId(toolId).size();
        int remaining = max - current;
        if (current >= max) {
            if (current > max) {
                redirectAttributes.addFlashAttribute("error",
                        "⚠️ Số lượng token hiện tại (" + current + ") vượt quá quantity mới (" + max + "). "
                                + "Vui lòng xóa bớt token trước khi thêm mới.");
            } else {
                redirectAttributes.addFlashAttribute("error",
                        "❌ Đã đủ số lượng (" + max + "). Không thể thêm nữa.");
            }
            return "redirect:/seller/tokens/manage/" + toolId;
        }

        List<String> clean = new ArrayList<>();
        for (String t : tokens) {
            if (t == null) continue;
            t = t.trim();
            if (!t.matches("^\\d{6}$") || licenseAccountRepository.existsByToken(t)) continue;
            clean.add(t);
        }
        if (clean.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "❌ Không có token hợp lệ để thêm.");
            return "redirect:/seller/tokens/manage/" + toolId;
        }
        if (clean.size() > remaining) {
            redirectAttributes.addFlashAttribute("error", "❌ Bạn chỉ có thể thêm tối đa " + remaining + " token nữa.");
            return "redirect:/seller/tokens/manage/" + toolId;
        }

        int added = 0;
        for (String t : clean) {
            LicenseAccount acc = new LicenseAccount();
            acc.setTool(tool);
            acc.setLoginMethod(LicenseAccount.LoginMethod.TOKEN);
            acc.setToken(t);
            acc.setUsed(false);
            acc.setStatus(LicenseAccount.Status.ACTIVE);
            acc.setStartDate(LocalDateTime.now());
            licenseAccountRepository.save(acc);
            added++;
        }

        redirectAttributes.addFlashAttribute("success", "✅ Đã thêm " + added + " token vào DB!");
        return "redirect:/seller/tokens/manage/" + toolId;
    }

    /** ============================================================
     * 🔹 4️⃣ Xóa token
     * ============================================================ */
    @PostMapping("/delete")
    @Transactional
    public String deleteToken(@RequestParam(value = "index", required = false) Integer index,
                              @RequestParam(value = "tokenId", required = false) Long tokenId,
                              @RequestParam(value = "toolId", required = false) Long toolId,
                              HttpServletRequest request,
                              RedirectAttributes redirectAttributes) {

        HttpSession session = request.getSession();

        // 🧩 SESSION MODE
        if (toolId == null) {
            @SuppressWarnings("unchecked")
            List<String> tempTokens = (List<String>) session.getAttribute("tempTokens");
            if (tempTokens != null && index != null && index >= 0 && index < tempTokens.size()) {
                tempTokens.remove((int) index);
                session.setAttribute("tempTokens", tempTokens);

                redirectAttributes.addFlashAttribute("success", "🗑 Xóa token tạm thành công!");
            } else {
                redirectAttributes.addFlashAttribute("error", "❌ Token không hợp lệ!");
            }
            return "redirect:/seller/tokens/manage";
        }

        // 💾 DB MODE
        Account seller = (Account) session.getAttribute("loggedInAccount");
        LicenseAccount token = licenseAccountRepository.findById(tokenId).orElse(null);
        if (token == null) {
            redirectAttributes.addFlashAttribute("error", "❌ Token không tồn tại.");
            return "redirect:/seller/tokens/manage/" + toolId;
        }

        if (!token.getTool().getSeller().getAccountId().equals(seller.getAccountId())) {
            redirectAttributes.addFlashAttribute("error", "❌ Không có quyền xóa token này.");
            return "redirect:/seller/tokens/manage/" + toolId;
        }
        if (token.getUsed()) {
            redirectAttributes.addFlashAttribute("error", "⚠️ Token này đã được khách hàng sử dụng, không thể xóa.");
            return "redirect:/seller/tokens/manage/" + toolId;
        }

        licenseAccountRepository.delete(token);

        redirectAttributes.addFlashAttribute("success", "🗑 Xóa token thành công!");
        return "redirect:/seller/tokens/manage/" + toolId;
    }

    /** ============================================================
     * 🔹 5️⃣ Finalize tool
     * ============================================================ */
    @PostMapping("/finalize")
    @Transactional
    public String finalizeTool(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        HttpSession session = request.getSession();

        // Lấy dữ liệu tạm
        Tool tempTool = (Tool) session.getAttribute("tempTool");
        @SuppressWarnings("unchecked")
        List<String> tempTokens = (List<String>) session.getAttribute("tempTokens");
        @SuppressWarnings("unchecked")
        List<Integer> licenseDays = (List<Integer>) session.getAttribute("licenseDays");
        @SuppressWarnings("unchecked")
        List<Double> licensePrices = (List<Double>) session.getAttribute("licensePrices");

        if (tempTool == null) {
            redirectAttributes.addFlashAttribute("error", "⚠️ Phiên thêm tool đã hết hạn.");
            return "redirect:/seller/tools/add";
        }
        if (tempTokens == null || tempTokens.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "❌ Bạn phải thêm ít nhất 1 token.");
            return "redirect:/seller/tokens/manage";
        }
        int max = tempTool.getQuantity() == null ? 0 : tempTool.getQuantity();
        int count = (tempTokens == null) ? 0 : tempTokens.size();
        if (count != max) {
            redirectAttributes.addFlashAttribute("error",
                    "Bạn đã thêm " + count + " token, nhưng quantity là " + max + ". Vui lòng điều chỉnh cho bằng nhau.");
            return "redirect:/seller/tokens/manage";
        }
        try {
            // Seller hiện tại
            Account seller = (Account) session.getAttribute("loggedInAccount");

            // ⛳️ Quantity = số lượng token
            tempTool.setQuantity(tempTokens.size());
            tempTool.setSeller(seller);
            tempTool.setStatus(Tool.Status.PENDING);
            tempTool.setLoginMethod(Tool.LoginMethod.TOKEN);
            tempTool.setCreatedAt(LocalDateTime.now());
            tempTool.setUpdatedAt(LocalDateTime.now());

            // Lưu tool thật
            Tool saved = toolService.addTool(tempTool, seller);

            // Lưu license (nếu có)
            if (licenseDays != null && licensePrices != null) {
                int n = Math.min(licenseDays.size(), licensePrices.size());
                for (int i = 0; i < n; i++) {
                    Integer d = licenseDays.get(i);
                    Double p = licensePrices.get(i);
                    if (d == null || p == null || d <= 0 || p < 0) continue;

                    License l = new License();
                    l.setTool(saved);
                    l.setDurationDays(d);
                    l.setPrice(p);
                    l.setName("License " + d + " days");
                    l.setCreatedAt(LocalDateTime.now());
                    licenseRepo.save(l);
                }
            }

            // Lưu các token thành LicenseAccount
            for (String token : tempTokens) {
                if (token == null) continue;
                String t = token.trim();
                if (!t.matches("^\\d{6}$")) continue;                   // chỉ nhận token 6 chữ số
                if (licenseAccountRepository.existsByToken(t)) continue; // tránh trùng DB

                LicenseAccount acc = new LicenseAccount();
                acc.setTool(saved);
                acc.setLoginMethod(LicenseAccount.LoginMethod.TOKEN);
                acc.setToken(t);
                acc.setUsed(false);
                acc.setStatus(LicenseAccount.Status.ACTIVE);
                acc.setStartDate(LocalDateTime.now());
                licenseAccountRepository.save(acc);
            }

            // Dọn session
            session.removeAttribute("tempTool");
            session.removeAttribute("tempTokens");
            session.removeAttribute("licenseDays");
            session.removeAttribute("licensePrices");

            redirectAttributes.addFlashAttribute(
                    "success",
                    "🎉 Tool '" + saved.getToolName() + "' created with " + tempTokens.size() + " tokens!"
            );
            return "redirect:/seller/tools";

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "❌ Lỗi: " + e.getMessage());
            return "redirect:/seller/tools/add";
        }
    }


    /** ============================================================
     * 🔹 6️⃣ Back
     * ============================================================ */
    @GetMapping("/back")
    public String backToToolAdd(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        HttpSession session = request.getSession();
        Tool tempTool = (Tool) session.getAttribute("tempTool");

        if (tempTool == null) {
            redirectAttributes.addFlashAttribute("error", "⚠️ Phiên tạo tool đã hết hạn!");
            return "redirect:/seller/tools/add";
        }
        return "redirect:/seller/tools/add";
    }
    /** ============================================================
     * 🔹 7️⃣ Finalize Edit Tool (khi quantity thay đổi)
     * ============================================================ */
    @PostMapping("/finalize-edit")
    @Transactional
    public String finalizeEditTool(@RequestParam("toolId") Long toolId,
                                   HttpServletRequest request,
                                   RedirectAttributes redirectAttributes) {
        HttpSession session = request.getSession();
        Tool editTemp = (Tool) session.getAttribute("editToolTemp");

        if (editTemp == null) {
            redirectAttributes.addFlashAttribute("error", "⚠️ Phiên sửa tool đã hết hạn.");
            return "redirect:/seller/tools";
        }

        Tool existing = toolRepository.findById(toolId)
                .orElseThrow(() -> new RuntimeException("Tool không tồn tại."));

        // Đếm token hiện có
        List<LicenseAccount> tokens = licenseAccountRepository.findByToolToolId(toolId);
        int count = tokens.size();
        int max = editTemp.getQuantity() == null ? 0 : editTemp.getQuantity();

        if (count != max) {
            redirectAttributes.addFlashAttribute("error",
                    "❌ Số lượng token (" + count + ") không khớp với quantity mới (" + max + ").");
            return "redirect:/seller/tokens/manage/" + toolId + "?mode=edit";
        }

        // Cập nhật thông tin tool thật
        existing.setToolName(editTemp.getToolName());
        existing.setDescription(editTemp.getDescription());
        existing.setCategory(editTemp.getCategory());
        existing.setQuantity(editTemp.getQuantity());
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setStatus(Tool.Status.PENDING);

        toolRepository.save(existing);

        // Xóa session tạm
        session.removeAttribute("editToolTemp");

        redirectAttributes.addFlashAttribute("success", "✅ Tool đã được cập nhật cùng với token mới!");
        return "redirect:/seller/tools";
    }

}