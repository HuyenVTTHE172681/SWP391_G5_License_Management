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

    // ==========================================================
    // 🔹 LẤY DANH SÁCH TOKEN
    // ==========================================================

    /** ✅ Lấy toàn bộ token (LicenseAccount) của một tool */
    public List<LicenseAccount> getTokensByTool(Long toolId) {
        return licenseAccountRepository.findByTool_ToolIdAndLoginMethod(
                toolId, LicenseAccount.LoginMethod.TOKEN
        );
    }

    // ==========================================================
    // 🔹 THÊM TOKEN CHO TOOL ĐÃ CÓ
    // ==========================================================

    /**
     * ✅ Thêm nhiều token cho Tool đã tồn tại trong DB
     * - Kiểm tra quyền sở hữu (seller)
     * - Validate format, chống trùng, không vượt quá quantity
     */
    public void addTokensToTool(Long toolId, List<String> tokens, Account seller) {
        Tool tool = toolRepository.findById(toolId)
                .orElseThrow(() -> new IllegalArgumentException("Tool not found."));

        if (!tool.getSeller().getAccountId().equals(seller.getAccountId())) {
            throw new IllegalArgumentException("Bạn không có quyền chỉnh sửa tool này.");
        }

        if (tokens == null || tokens.isEmpty()) {
            throw new IllegalArgumentException("Danh sách token trống.");
        }

        // Số token còn trống có thể thêm
        List<LicenseAccount> existing = licenseAccountRepository
                .findByTool_ToolIdAndLoginMethod(toolId, LicenseAccount.LoginMethod.TOKEN);

        int current = existing.size();
        int quantity = tool.getQuantity() != null ? tool.getQuantity() : 0;
        int remaining = quantity - current;

        if (remaining <= 0) {
            throw new IllegalArgumentException("Đã đủ số lượng token (" + quantity + ").");
        }
        if (tokens.size() > remaining) {
            throw new IllegalArgumentException("Chỉ có thể thêm tối đa " + remaining + " token nữa.");
        }

        // Validate & thêm
        for (String token : tokens) {
            if (token == null || !token.matches("^\\d{6}$")) {
                throw new IllegalArgumentException("Token không hợp lệ: '" + token + "' (phải 6 chữ số)");
            }
            if (licenseAccountRepository.existsByToolAndToken(tool, token)) {
                throw new IllegalArgumentException("Token '" + token + "' đã tồn tại trong tool này.");
            }

            LicenseAccount acc = new LicenseAccount();
            acc.setTool(tool);
            acc.setLoginMethod(LicenseAccount.LoginMethod.TOKEN);
            acc.setToken(token);
            acc.setStatus(LicenseAccount.Status.ACTIVE);
            acc.setCreatedAt(LocalDateTime.now());
            licenseAccountRepository.save(acc);
        }
    }

    // ==========================================================
    // 🔹 XOÁ TOKEN
    // ==========================================================

    /** ✅ Xoá một token (chỉ khi chưa dùng / còn active) */
    public void deleteToken(Long tokenId) {
        if (!licenseAccountRepository.existsById(tokenId)) {
            throw new IllegalArgumentException("Token không tồn tại.");
        }
        licenseAccountRepository.deleteById(tokenId);
    }

    // ==========================================================
    // 🔹 FINALIZE TOOL TỪ PHIÊN (FLOW 2 BƯỚC)
    // ==========================================================

    /**
     * ✅ Finalize Tool (khi seller đã nhập token sau khi tạo tool tạm)
     * - Tạo Tool, Licenses, và Tokens cùng lúc
     */
    public Tool finalizeTool(Account seller,
                             Tool tempTool,
                             List<String> tokens,
                             List<Integer> licenseDays,
                             List<Double> licensePrices) {

        if (tempTool == null)
            throw new IllegalArgumentException("Phiên tool tạm không tồn tại.");

        if (tokens == null || tokens.isEmpty())
            throw new IllegalArgumentException("Vui lòng nhập danh sách token.");

        if (licenseDays == null || licensePrices == null ||
                licenseDays.isEmpty() || licensePrices.isEmpty()) {
            throw new IllegalArgumentException("Thiếu thông tin license.");
        }

        if (licenseDays.size() != licensePrices.size()) {
            throw new IllegalArgumentException("Danh sách ngày và giá không khớp nhau.");
        }

        if (tempTool.getQuantity() == null)
            throw new IllegalArgumentException("Số lượng tool không hợp lệ.");

        int expected = tempTool.getQuantity();
        int actual = tokens.size();

        if (actual < expected) {
            throw new IllegalArgumentException("Thiếu token. Cần thêm " + (expected - actual) + " token.");
        }
        if (actual > expected) {
            throw new IllegalArgumentException("Thừa token. Cần xoá bớt " + (actual - expected) + " token.");
        }

        // Cập nhật metadata
        tempTool.setSeller(seller);
        tempTool.setStatus(Tool.Status.PENDING);
        tempTool.setCreatedAt(LocalDateTime.now());
        tempTool.setUpdatedAt(LocalDateTime.now());

        // ✅ Lưu Tool
        Tool saved = toolRepository.save(tempTool);

        // ✅ Lưu Licenses
        List<License> licenseEntities = new ArrayList<>();
        for (int i = 0; i < licenseDays.size(); i++) {
            License l = new License();
            l.setTool(saved);
            l.setDurationDays(licenseDays.get(i));
            l.setPrice(licensePrices.get(i));
            l.setCreatedAt(LocalDateTime.now());
            licenseEntities.add(l);
        }
        licenseRepository.saveAll(licenseEntities);

        // ✅ Lưu Tokens
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

    // ==========================================================
    // 🔹 CẬP NHẬT TOKEN KHI EDIT TOOL (FLOW TOKEN-EDIT)
    // ==========================================================

    /** ✅ Xoá toàn bộ token cũ và ghi lại token mới (finalize edit) */
    @Transactional
    public void updateTokensForTool(Tool tool, List<String> tokens) {
        licenseAccountRepository.deleteByTool(tool);

        for (String token : tokens) {
            if (token == null || !token.matches("^\\d{6}$")) {
                throw new IllegalArgumentException("Token không hợp lệ: '" + token + "' (phải 6 chữ số)");
            }

            LicenseAccount acc = new LicenseAccount();
            acc.setTool(tool);
            acc.setLoginMethod(LicenseAccount.LoginMethod.TOKEN);
            acc.setToken(token);
            acc.setStatus(LicenseAccount.Status.ACTIVE);
            acc.setCreatedAt(LocalDateTime.now());
            licenseAccountRepository.save(acc);
        }
    }
}
