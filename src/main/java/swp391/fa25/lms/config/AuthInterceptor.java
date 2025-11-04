package swp391.fa25.lms.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;

public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);

        // Kiểm tra đăng nhập
        if (session == null || session.getAttribute("loggedInAccount") == null) {
            // Nếu chưa đăng nhập → chuyển hướng đến trang login
            response.sendRedirect(request.getContextPath() + "/login");
            return false; // Dừng request ở đây
        }
        return true;
    }
}
