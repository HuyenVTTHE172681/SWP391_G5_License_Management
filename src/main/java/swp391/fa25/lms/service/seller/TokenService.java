package swp391.fa25.lms.service.seller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.fa25.lms.model.*;
import swp391.fa25.lms.repository.LicenseAccountRepository;
import swp391.fa25.lms.repository.LicenseToolRepository;
import swp391.fa25.lms.repository.ToolRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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
        return licenseAccountRepository.findByLicense_Tool_ToolId(toolId);
    }

    // ==========================================================
    // 🔹 THÊM TOKEN CHO TOOL
    // ==========================================================

    public void addTokensToTool(Long toolId, List<String> tokens, Account seller) {
        Tool tool = toolRepository.findById(toolId)
                .orElseThrow(() -> new IllegalArgumentException("Tool not found."));

        if (!tool.getSeller().getAccountId().equals(seller.getAccountId())) {
            throw new IllegalArgumentException("Bạn không có quyền chỉnh sửa tool này.");
        }

        if (tokens == null || tokens.isEmpty()) {
            throw new IllegalArgumentException("Danh sách token trống.");
        }

        List<LicenseAccount> existing = licenseAccountRepository.findByLicense_Tool_ToolId(toolId);
        int current = existing.size();
        int quantity = Optional.ofNullable(tool.getQuantity()).orElse(0);
        int remaining = quantity - current;

        if (remaining <= 0) {
            throw new IllegalArgumentException("Đã đủ số lượng token (" + quantity + ").");
        }
        if (tokens.size() > remaining) {
            throw new IllegalArgumentException("Chỉ có thể thêm tối đa " + remaining + " token nữa.");
        }

        // 🔹 Lấy license đầu tiên (nếu tool có nhiều license, bạn có thể thay đổi logic này)
        List<License> licenses = licenseRepository.findByTool_ToolId(toolId);
        if (licenses.isEmpty()) throw new IllegalArgumentException("Tool chưa có license.");
        License license = licenses.get(0);

        for (String token : tokens) {
            if (token == null || !token.matches("^\\d{6}$")) {
                throw new IllegalArgumentException("Token không hợp lệ: '" + token + "' (phải 6 chữ số)");
            }
            if (licenseAccountRepository.existsByLicense_Tool_ToolIdAndToken(toolId, token)) {
                throw new IllegalArgumentException("Token '" + token + "' đã tồn tại trong tool này.");
            }

            LicenseAccount acc = new LicenseAccount();
            acc.setLicense(license);
            acc.setToken(token);
            acc.setStatus(LicenseAccount.Status.ACTIVE);
            licenseAccountRepository.save(acc);
        }
    }

    // ==========================================================
    // 🔹 XOÁ TOKEN
    // ==========================================================

    public void deleteToken(Long tokenId) {
        if (!licenseAccountRepository.existsById(tokenId)) {
            throw new IllegalArgumentException("Token không tồn tại.");
        }
        licenseAccountRepository.deleteById(tokenId);
    }

    // ==========================================================
    // 🔹 FINALIZE TOOL (TẠO TOOL + TOKEN)
    // ==========================================================

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

        int expected = Optional.ofNullable(tempTool.getQuantity()).orElse(0);
        int actual = tokens.size();
        if (expected <= 0) throw new IllegalArgumentException("Số lượng tool không hợp lệ.");
        if (actual != expected)
            throw new IllegalArgumentException("Số lượng token không khớp (" + actual + "/" + expected + ").");

        tempTool.setSeller(seller);
        tempTool.setStatus(Tool.Status.PENDING);
        tempTool.setCreatedAt(LocalDateTime.now());
        tempTool.setUpdatedAt(LocalDateTime.now());

        Tool saved = toolRepository.save(tempTool);

        // 🔹 Lưu licenses
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

        // 🔹 Gắn token vào license đầu tiên
        License primaryLicense = licenseEntities.get(0);
        for (String token : tokens) {
            if (!token.matches("^\\d{6}$"))
                throw new IllegalArgumentException("Token không hợp lệ: '" + token + "'");
            LicenseAccount acc = new LicenseAccount();
            acc.setLicense(primaryLicense);
            acc.setToken(token);
            acc.setStatus(LicenseAccount.Status.ACTIVE);
            licenseAccountRepository.save(acc);
        }

        return saved;
    }

    // ==========================================================
    // 🔹 CẬP NHẬT TOKEN KHI EDIT TOOL
    // ==========================================================

    @Transactional
    public void updateTokensForTool(Tool tool, List<String> tokens) {
        if (tool == null)
            throw new IllegalArgumentException("Tool không hợp lệ (null).");

        if (tokens == null || tokens.isEmpty())
            throw new IllegalArgumentException("Danh sách token trống.");

        List<LicenseAccount> existingAccounts = licenseAccountRepository.findByLicense_Tool_ToolId(tool.getToolId());
        Set<String> existingTokens = existingAccounts.stream()
                .map(LicenseAccount::getToken)
                .collect(Collectors.toSet());

        // 🔹 Xoá token cũ
        for (LicenseAccount acc : existingAccounts) {
            if (!tokens.contains(acc.getToken())) {
                licenseAccountRepository.delete(acc);
            }
        }

        // 🔹 Thêm token mới
        List<License> licenses = licenseRepository.findByTool_ToolId(tool.getToolId());
        if (licenses.isEmpty()) throw new IllegalArgumentException("Tool chưa có license.");
        License license = licenses.get(0);

        for (String token : tokens) {
            if (!token.matches("^\\d{6}$"))
                throw new IllegalArgumentException("Token không hợp lệ: '" + token + "'");
            if (existingTokens.contains(token)) continue;

            LicenseAccount newAcc = new LicenseAccount();
            newAcc.setLicense(license);
            newAcc.setToken(token);
            newAcc.setStatus(LicenseAccount.Status.ACTIVE);
            licenseAccountRepository.save(newAcc);
        }
    }
}