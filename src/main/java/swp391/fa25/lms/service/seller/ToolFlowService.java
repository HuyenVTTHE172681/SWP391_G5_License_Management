package swp391.fa25.lms.service.seller;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import swp391.fa25.lms.model.*;
import swp391.fa25.lms.repository.LicenseAccountRepository;
import swp391.fa25.lms.repository.LicenseToolRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ToolFlowService {

    @Autowired private TokenService tokenService;
    @Autowired private ToolService toolService;
    @Autowired private FileStorageService fileStorageService;
    @Autowired private LicenseAccountRepository licenseAccountRepository;
    @Autowired private LicenseToolRepository licenseRepository;

    private static final String SESSION_PENDING_TOOL = "pendingTool";
    private static final String SESSION_PENDING_EDIT = "pendingEditTool";

    // ==========================================================
    // 🔹 FLOW 1: TẠO TOOL MỚI
    // ==========================================================

    public void startCreateTool(
            Tool tool,
            MultipartFile imageFile,
            MultipartFile toolFile,
            Long categoryId,
            List<Integer> licenseDays,
            List<Double> licensePrices,
            HttpSession session
    ) throws IOException {

        Account seller = (Account) session.getAttribute("loggedInAccount");
        if (seller == null)
            throw new IllegalStateException("You must be logged in as a seller.");

        if (toolService.existsByToolName(tool.getToolName()))
            throw new IllegalArgumentException("Tool name already exists.");

        Category category = toolService.getCategoryById(categoryId);

        // ✅ Upload file ảnh + tool
        String imagePath = fileStorageService.uploadImage(imageFile);
        String toolPath = fileStorageService.uploadToolFile(toolFile);

        // ✅ File entity
        ToolFile fileEntity = new ToolFile();
        fileEntity.setFilePath(toolPath);
        fileEntity.setFileType(ToolFile.FileType.ORIGINAL);
        fileEntity.setUploadedBy(seller);
        fileEntity.setCreatedAt(LocalDateTime.now());
        fileEntity.setTool(tool);

        tool.setFiles(List.of(fileEntity));
        tool.setImage(imagePath);
        tool.setCategory(category);
        tool.setSeller(seller);
        tool.setStatus(Tool.Status.PENDING);

        // ✅ Licenses
        List<License> licenses = new ArrayList<>();
        for (int i = 0; i < licenseDays.size(); i++) {
            License l = new License();
            l.setName("License " + licenseDays.get(i) + " days");
            l.setDurationDays(licenseDays.get(i));
            l.setPrice(licensePrices.get(i));
            l.setTool(tool);
            licenses.add(l);
        }

        // ✅ Nếu login method = TOKEN thì tạm lưu session (đợi nhập token)
        if (tool.getLoginMethod() == Tool.LoginMethod.TOKEN) {
            session.setAttribute(SESSION_PENDING_TOOL,
                    new ToolSessionData(tool, category, licenses, toolPath, new ArrayList<>()));
            return;
        }

        // ✅ USER_PASSWORD → lưu ngay
        Tool saved = toolService.createTool(tool, category);
        toolService.createLicensesForTool(saved, licenses);
    }

    public void finalizeTokenTool(List<String> tokens, HttpSession session) throws IOException {
        ToolSessionData pending = (ToolSessionData) session.getAttribute(SESSION_PENDING_TOOL);
        if (pending == null)
            throw new IllegalStateException("No pending tool found in session.");

        Tool tool = pending.getTool();
        Category category = pending.getCategory();
        List<License> licenses = pending.getLicenses();

        if (tool == null)
            throw new IllegalStateException("No tool data found in session.");

        int expectedQuantity = tool.getQuantity();
        int actualQuantity = tokens.size();

        if (actualQuantity != expectedQuantity) {
            throw new IllegalArgumentException(
                    String.format("Quantity mismatch: expected %d tokens, but got %d.", expectedQuantity, actualQuantity)
            );
        }

        for (String token : tokens) {
            if (licenseAccountRepository.existsByToken(token)) {
                throw new IllegalArgumentException("Duplicate token detected: " + token);
            }
        }

        Tool saved = toolService.createTool(tool, category);
        toolService.createLicensesForTool(saved, licenses);

        // ✅ Lưu tokens cho license đầu tiên
        List<License> savedLicenses = licenseRepository.findByTool_ToolId(saved.getToolId());
        if (savedLicenses.isEmpty())
            throw new IllegalStateException("No licenses found for tool.");

        License primaryLicense = savedLicenses.get(0);

        for (String token : tokens) {
            LicenseAccount acc = new LicenseAccount();
            acc.setLicense(primaryLicense);
            acc.setToken(token);
            acc.setStatus(LicenseAccount.Status.ACTIVE);
            licenseAccountRepository.save(acc);
        }

        session.removeAttribute(SESSION_PENDING_TOOL);
    }

    // ==========================================================
    // 🔹 FLOW 2: EDIT TOOL (TOKEN)
    // ==========================================================

    public void startEditToolSession(
            Tool existingTool,
            Tool updatedTool,
            MultipartFile imageFile,
            MultipartFile toolFile,
            List<Integer> licenseDays,
            List<Double> licensePrices,
            HttpSession session
    ) throws IOException {

        // ✅ Upload ảnh nếu có
        if (imageFile != null && !imageFile.isEmpty()) {
            updatedTool.setImage(fileStorageService.uploadImage(imageFile));
        } else {
            updatedTool.setImage(existingTool.getImage());
        }

        // ✅ Upload file tool nếu có
        List<ToolFile> updatedFiles = new ArrayList<>();
        if (toolFile != null && !toolFile.isEmpty()) {
            String newToolPath = fileStorageService.uploadToolFile(toolFile);
            ToolFile fileEntity = new ToolFile();
            fileEntity.setFilePath(newToolPath);
            fileEntity.setFileType(ToolFile.FileType.ORIGINAL);
            fileEntity.setUploadedBy(existingTool.getSeller());
            fileEntity.setCreatedAt(LocalDateTime.now());
            fileEntity.setTool(existingTool);
            updatedFiles.add(fileEntity);
        } else {
            updatedFiles = existingTool.getFiles();
        }

        updatedTool.setFiles(updatedFiles);

        // ✅ Tạo license mới
        List<License> licenses = new ArrayList<>();
        for (int i = 0; i < licenseDays.size(); i++) {
            License license = new License();
            license.setName("License " + licenseDays.get(i) + " days");
            license.setDurationDays(licenseDays.get(i));
            license.setPrice(licensePrices.get(i));
            licenses.add(license);
        }

        // ✅ Lấy token hiện có
        List<LicenseAccount> existingTokens =
                licenseAccountRepository.findByLicense_Tool_ToolId(existingTool.getToolId());

        List<String> tokenValues = existingTokens.stream()
                .map(LicenseAccount::getToken)
                .collect(Collectors.toCollection(ArrayList::new));

        session.setAttribute(
                SESSION_PENDING_EDIT,
                new ToolSessionData(existingTool, updatedTool.getCategory(), licenses, null, tokenValues)
        );
    }

    @Transactional
    public void finalizeEditTokenTool(List<String> tokens, HttpSession session) {
        ToolSessionData pending = (ToolSessionData) session.getAttribute(SESSION_PENDING_EDIT);
        if (pending == null)
            throw new IllegalStateException("No tool edit data found in session.");

        Tool tool = pending.getTool();
        if (tool == null)
            throw new IllegalStateException("Tool data missing in session.");

        Long currentToolId = tool.getToolId();

        if (tokens == null || tokens.isEmpty()) {
            throw new IllegalArgumentException("Danh sách token trống. Vui lòng thêm ít nhất 1 token.");
        }

        // ✅ Kiểm tra trùng token
        for (String token : tokens) {
            if (token == null || !token.matches("^\\d{6}$")) {
                throw new IllegalArgumentException("Token không hợp lệ: '" + token + "'");
            }

            LicenseAccount existing = licenseAccountRepository.findByToken(token);
            if (existing != null && !existing.getLicense().getTool().getToolId().equals(currentToolId)) {
                throw new IllegalArgumentException("Token '" + token + "' đã tồn tại trong tool khác!");
            }
        }

        // ✅ Đồng bộ token
        tokenService.updateTokensForTool(tool, tokens);

        int newQuantity = tokens.size();
        tool.setQuantity(newQuantity);
        toolService.updateQuantityAndLicenses(tool.getToolId(), newQuantity, pending.getLicenses());

        session.removeAttribute(SESSION_PENDING_EDIT);
    }

    // ==========================================================
    // 🔹 Utility
    // ==========================================================

    public void cancelToolCreation(HttpSession session) {
        session.removeAttribute(SESSION_PENDING_TOOL);
        session.removeAttribute(SESSION_PENDING_EDIT);
    }

    // ==========================================================
    // 🔹 Session DTO
    // ==========================================================

    public static class ToolSessionData {
        private final Tool tool;
        private final Category category;
        private final List<License> licenses;
        private final String filePath;
        private final List<String> tokens;

        public ToolSessionData(Tool tool, Category category, List<License> licenses, String filePath, List<String> tokens) {
            this.tool = tool;
            this.category = category;
            this.licenses = licenses;
            this.filePath = filePath;
            this.tokens = tokens;
        }

        public Tool getTool() { return tool; }
        public Category getCategory() { return category; }
        public List<License> getLicenses() { return licenses; }
        public String getFilePath() { return filePath; }
        public List<String> getTokens() { return tokens; }
    }
}