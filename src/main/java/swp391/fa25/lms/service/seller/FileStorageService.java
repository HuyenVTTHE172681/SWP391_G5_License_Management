package swp391.fa25.lms.service.seller;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageService {

    // ✅ Thư mục lưu ảnh và file tool
    private static final String IMAGE_DIR = "uploads/images";
    private static final String TOOL_DIR = "uploads/tools";

    // ✅ Cho phép ảnh định dạng PNG hoặc JPG
    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/jpg"
    );

    // ✅ Chỉ cho phép file .zip, .rar, .exe
    private static final List<String> ALLOWED_TOOL_EXTENSIONS = Arrays.asList(".zip", ".rar", ".exe");

    /**
     * ✅ Upload ảnh đại diện của tool
     */
    public String uploadImage(MultipartFile imageFile) throws IOException {
        validateMimeType(imageFile, ALLOWED_IMAGE_TYPES, "image");
        return saveFile(imageFile, IMAGE_DIR);
    }

    /**
     * ✅ Upload file tool (.zip, .rar, .exe)
     */
    public String uploadToolFile(MultipartFile toolFile) throws IOException {
        validateExtension(toolFile, ALLOWED_TOOL_EXTENSIONS, "tool file");
        return saveFile(toolFile, TOOL_DIR);
    }

    /**
     * ✅ Validate định dạng file ảnh bằng MIME type
     */
    private void validateMimeType(MultipartFile file, List<String> allowedTypes, String fileTypeName) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please upload a " + fileTypeName + ".");
        }
        String contentType = file.getContentType();
        if (contentType == null || !allowedTypes.contains(contentType)) {
            throw new IllegalArgumentException("Invalid " + fileTypeName + " format. Only " + allowedTypes + " allowed.");
        }
    }

    /**
     * ✅ Validate phần mở rộng file (đuôi .zip, .rar, .exe)
     */
    private void validateExtension(MultipartFile file, List<String> allowedExtensions, String fileTypeName) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please upload a " + fileTypeName + ".");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            throw new IllegalArgumentException("File name is invalid.");
        }

        // Lấy phần mở rộng của file
        String lowerName = originalName.toLowerCase();
        boolean valid = allowedExtensions.stream().anyMatch(lowerName::endsWith);

        if (!valid) {
            throw new IllegalArgumentException("Invalid " + fileTypeName + " extension. Only " + allowedExtensions + " allowed.");
        }
    }

    /**
     * ✅ Lưu file và trả về đường dẫn tương đối
     */
    private String saveFile(MultipartFile file, String directory) throws IOException {
        Path uploadDir = Paths.get(directory);
        Files.createDirectories(uploadDir);

        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : "";

        String newFilename = UUID.randomUUID() + extension;
        Path targetPath = uploadDir.resolve(newFilename);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        return directory + "/" + newFilename;
    }
}
