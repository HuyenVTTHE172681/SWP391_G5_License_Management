package swp391.fa25.lms.service.seller;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import swp391.fa25.lms.model.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class ToolFlowService {

    @Autowired private ToolService toolService;
    @Autowired private FileStorageService fileStorageService;

    // ✅ key lưu session tạm thời cho tool chưa hoàn thành
    private static final String SESSION_PENDING_TOOL = "pendingTool";

    /**
     * ✅ Bắt đầu tạo tool mới
     * 1️⃣ Validate logic nghiệp vụ
     * 2️⃣ Upload file (ảnh + tool)
     * 3️⃣ Lưu Tool trạng thái PENDING
     * 4️⃣ Nếu chọn loginMethod = TOKEN → lưu tạm Tool trong session và chờ token
     * 5️⃣ Nếu chọn USER_PASSWORD → lưu luôn DB
     */
    public void startCreateTool(Tool tool,
                                MultipartFile imageFile,
                                MultipartFile toolFile,
                                Long categoryId,
                                List<Integer> licenseDays,
                                List<Double> licensePrices,
                                HttpSession session) throws IOException {

        // 🔹 Lấy seller từ session
        Account seller = (Account) session.getAttribute("loggedInAccount");
        if (seller == null) {
            throw new IllegalStateException("You must be logged in as a seller to add a tool.");
        }

        // 🔹 Validate trùng tên
        if (toolService.existsByToolName(tool.getToolName())) {
            throw new IllegalArgumentException("Tool name already exists!");
        }

        // 🔹 Lấy category
        Category category = toolService.getCategoryById(categoryId);
        if (category == null) {
            throw new IllegalArgumentException("Category not found");
        }

        // 🔹 Upload file ảnh + tool
        String imagePath = fileStorageService.uploadImage(imageFile);
        String toolPath = fileStorageService.uploadToolFile(toolFile);

        tool.setImage(imagePath);
        tool.setCategory(category);
        tool.setSeller(seller); // ✅ GẮN SELLER Ở ĐÂY
        tool.setStatus(Tool.Status.PENDING); // (khuyến nghị) đặt mặc định là Pending khi mới tạo

        // 🔹 Tạo danh sách license
        List<License> licenses = new ArrayList<>();
        for (int i = 0; i < licenseDays.size(); i++) {
            License license = new License();
            license.setDurationDays(licenseDays.get(i));
            license.setPrice(licensePrices.get(i));
            license.setName("License " + licenseDays.get(i) + " days"); // ✅ tránh lỗi validation
            licenses.add(license);
        }

        // 🔹 Nếu là TOKEN → lưu tạm Tool & License vào session để xử lý sau
        if (tool.getLoginMethod() == Tool.LoginMethod.TOKEN) {
            ToolSessionData pending = new ToolSessionData(tool, category, licenses, toolPath);
            session.setAttribute(SESSION_PENDING_TOOL, pending);
            return;
        }

        // 🔹 Nếu là USER_PASSWORD → lưu luôn vào DB
        Tool saved = toolService.createTool(tool, category);
        toolService.createLicensesForTool(saved, licenses);

        // (Nếu bạn có entity ToolFile riêng thì tạo ở đây)
    }

    /**
     * ✅ Sau khi user nhập token ở token-manage
     * - Lấy ToolSessionData từ session
     * - So sánh quantity và số lượng token
     * - Nếu khớp → lưu Tool + Token vào DB
     */
    public void finalizeTokenTool(List<String> tokens, HttpSession session) throws IOException {
        ToolSessionData pending = (ToolSessionData) session.getAttribute(SESSION_PENDING_TOOL);
        if (pending == null) {
            throw new IllegalStateException("No pending tool found in session.");
        }

        Tool tool = pending.getTool();
        Category category = pending.getCategory();
        List<License> licenses = pending.getLicenses();

        int expected = tool.getQuantity();
        int actual = tokens.size();

        if (actual < expected) {
            throw new IllegalArgumentException("Not enough tokens. Need " + (expected - actual) + " more.");
        } else if (actual > expected) {
            throw new IllegalArgumentException("Too many tokens. Remove " + (actual - expected) + " extra tokens.");
        }

        // ✅ Lưu tool vào DB
        Tool saved = toolService.createTool(tool, category);

        // ✅ Lưu license
        toolService.createLicensesForTool(saved, licenses);

        // ✅ Lưu token
        toolService.createLicenseAccountsForTool(saved, tokens);

        // ✅ Clear session
        session.removeAttribute(SESSION_PENDING_TOOL);
    }

    /**
     * ✅ Khi người dùng bấm “Back” ở token-manage → hủy luồng tạm và quay lại tool-add
     */
    public void cancelToolCreation(HttpSession session) {
        // Xóa tool tạm đang lưu trong session
        session.removeAttribute(SESSION_PENDING_TOOL);
    }
    /**
     * ✅ Dùng để xem tool tạm thời trong session (debug)
     */
    public ToolSessionData getPendingTool(HttpSession session) {
        return (ToolSessionData) session.getAttribute(SESSION_PENDING_TOOL);
    }

    /**
     * ✅ Lớp tạm lưu dữ liệu tool khi đang chờ token
     */
    public static class ToolSessionData {
        private final Tool tool;
        private final Category category;
        private final List<License> licenses;
        private final String toolPath;

        public ToolSessionData(Tool tool, Category category, List<License> licenses, String toolPath) {
            this.tool = tool;
            this.category = category;
            this.licenses = licenses;
            this.toolPath = toolPath;
        }

        public Tool getTool() { return tool; }
        public Category getCategory() { return category; }
        public List<License> getLicenses() { return licenses; }
        public String getToolPath() { return toolPath; }
    }
}
