package swp391.fa25.lms.service.seller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.fa25.lms.model.*;
import swp391.fa25.lms.repository.LicenseAccountRepository;
import swp391.fa25.lms.repository.LicenseToolRepository;
import swp391.fa25.lms.repository.ToolRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class TokenService {

    @Autowired private LicenseAccountRepository licenseAccountRepository;
    @Autowired private ToolRepository toolRepository;
    @Autowired private LicenseToolRepository licenseRepository;

    /**
     * ✅ Lấy toàn bộ token (LicenseAccount) của một tool
     */
    public List<LicenseAccount> getTokensByTool(Long toolId) {
        return licenseAccountRepository.findByTool_ToolId(toolId);
    }

    /**
     * ✅ Thêm nhiều token cho tool đã tồn tại trong DB (DB mode)
     * - Kiểm tra quyền sở hữu tool (seller)
     * - Validate 6 chữ số, không trùng, không vượt quá quantity
     */
    public void addTokensToTool(Long toolId, List<String> tokens, Account seller) {
        Tool tool = toolRepository.findById(toolId)
                .orElseThrow(() -> new IllegalArgumentException("Tool not found"));

        if (!tool.getSeller().getAccountId().equals(seller.getAccountId())) {
            throw new IllegalArgumentException("Bạn không có quyền sửa tool này");
        }

        if (tokens == null || tokens.isEmpty()) {
            throw new IllegalArgumentException("Danh sách token trống");
        }

        // Hiện có bao nhiêu token rồi?
        List<LicenseAccount> existing = licenseAccountRepository.findByTool_ToolId(toolId);
        int current = existing.size();
        int quantity = tool.getQuantity() == null ? 0 : tool.getQuantity();
        int remaining = quantity - current;

        if (remaining <= 0) {
            throw new IllegalArgumentException("Đã đủ số lượng token (" + quantity + ")");
        }
        if (tokens.size() > remaining) {
            throw new IllegalArgumentException("Chỉ có thể thêm tối đa " + remaining + " token nữa");
        }

        // Validate từng token & chống trùng
        for (String t : tokens) {
            if (t == null || !t.matches("^\\d{6}$")) {
                throw new IllegalArgumentException("Token không hợp lệ: '" + t + "' (phải 6 chữ số)");
            }
            if (licenseAccountRepository.existsByToolAndToken(tool, t)) {
                throw new IllegalArgumentException("Token '" + t + "' đã tồn tại trong tool này");
            }
        }

        // Lưu
        for (String token : tokens) {
            LicenseAccount acc = new LicenseAccount();
            acc.setTool(tool);
            acc.setLoginMethod(LicenseAccount.LoginMethod.TOKEN);
            acc.setToken(token);
            acc.setStatus(LicenseAccount.Status.ACTIVE);
            acc.setCreatedAt(LocalDateTime.now());
            licenseAccountRepository.save(acc);
        }
    }

    /**
     * ✅ Xoá 1 token (DB mode)
     * (Tuân thủ logic business: chỉ cho xoá khi phù hợp, ví dụ chưa used...)
     */
    public void deleteToken(Long tokenId) {
        LicenseAccount acc = licenseAccountRepository.findById(tokenId)
                .orElseThrow(() -> new IllegalArgumentException("Token không tồn tại"));
        // Nếu có ràng buộc business khác (VD: acc.getUsed() == true thì không xoá) thì kiểm tra tại đây
        licenseAccountRepository.delete(acc);
    }

    /**
     * ✅ Finalize từ phiên tạm: tạo Tool + Licenses + Tokens trong 1 transaction
     *  - Dùng khi flow 2 bước (tool-add → token-manage → finalize)
     *  - Giữ nguyên nguyên tắc: Tool sau khi tạo luôn PENDING
     */
    public Tool finalizeTool(Account seller,
                             Tool tempTool,
                             List<String> tokens,
                             List<Integer> licenseDays,
                             List<Double> licensePrices) {

        if (tempTool == null) throw new IllegalArgumentException("Phiên tool tạm không tồn tại");
        if (tokens == null || tokens.isEmpty()) throw new IllegalArgumentException("Vui lòng nhập token");
        if (licenseDays == null || licensePrices == null || licenseDays.isEmpty() || licensePrices.isEmpty()) {
            throw new IllegalArgumentException("Thiếu gói license");
        }
        if (licenseDays.size() != licensePrices.size()) {
            throw new IllegalArgumentException("Danh sách days/price không khớp nhau");
        }
        if (tempTool.getQuantity() == null) {
            throw new IllegalArgumentException("Quantity không hợp lệ");
        }

        // So khớp số lượng token với quantity
        int expected = tempTool.getQuantity();
        int actual = tokens.size();
        if (actual < expected) {
            throw new IllegalArgumentException("Thiếu token. Cần thêm " + (expected - actual) + " token");
        }
        if (actual > expected) {
            throw new IllegalArgumentException("Thừa token. Cần xoá bớt " + (actual - expected) + " token");
        }

        // Gán seller & trạng thái
        tempTool.setSeller(seller);
        tempTool.setStatus(Tool.Status.PENDING); // luôn Pending sau khi tạo mới
        tempTool.setCreatedAt(LocalDateTime.now());
        tempTool.setUpdatedAt(LocalDateTime.now());

        // Lưu Tool
        Tool saved = toolRepository.save(tempTool);

        // Lưu Licenses
        List<License> toSave = new ArrayList<>();
        for (int i = 0; i < licenseDays.size(); i++) {
            License l = new License();
            l.setTool(saved);
            l.setDurationDays(licenseDays.get(i));
            l.setPrice(licensePrices.get(i));
            l.setCreatedAt(LocalDateTime.now());
            toSave.add(l);
        }
        licenseRepository.saveAll(toSave);

        // Lưu Tokens thành LicenseAccount
        for (String token : tokens) {
            if (!token.matches("^\\d{6}$")) {
                throw new IllegalArgumentException("Token không hợp lệ: '" + token + "' (phải 6 chữ số)");
            }
            LicenseAccount acc = new LicenseAccount();
            acc.setTool(saved);
            acc.setLoginMethod(LicenseAccount.LoginMethod.TOKEN);
            acc.setToken(token);
            acc.setStatus(LicenseAccount.Status.ACTIVE);
            acc.setCreatedAt(LocalDateTime.now());
            licenseAccountRepository.save(acc);
        }

        return saved;
    }
}
