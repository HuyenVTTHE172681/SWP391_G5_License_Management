package swp391.fa25.lms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class  G5LicenseManagementSystemApplication {

    public static void main(String[] args) {

        SpringApplication.run(G5LicenseManagementSystemApplication.class, args);
        System.out.println("📂 Working directory = " + System.getProperty("user.dir"));
    }

}
