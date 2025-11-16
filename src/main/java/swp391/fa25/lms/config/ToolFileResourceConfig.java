package swp391.fa25.lms.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ToolFileResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        // Thư mục thật chứa file tool
        String toolUploadPath = System.getProperty("user.dir")
                + "/SWP391_G5_License_Management/uploads/tools/";

        registry.addResourceHandler("/tool-files/**")
                .addResourceLocations("file:" + toolUploadPath);
    }
}
