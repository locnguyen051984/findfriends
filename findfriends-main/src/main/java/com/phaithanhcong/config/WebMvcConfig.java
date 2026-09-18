package com.phaithanhcong.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;
import com.phaithanhcong.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadDir = "D:/findfriends-data/uploads/";
        Path uploadPath = Paths.get(uploadDir);

        // Cấu hình đường dẫn truy cập file tĩnh qua URL /uploads/**
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:/" + uploadPath.toAbsolutePath().toString().replace("\\", "/") + "/");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {

            @Override
            public boolean preHandle(
                    HttpServletRequest request,
                    HttpServletResponse response,
                    Object handler) throws Exception {

                String uri = request.getRequestURI();

                // Những URL không cần đăng nhập
                if (uri.equals("/")
                        || uri.equals("/login")
                        || uri.equals("/register")
                        || uri.equals("/forgot-password")
                        || uri.equals("/reset-password")
                        || uri.startsWith("/css/")
                        || uri.startsWith("/js/")
                        || uri.startsWith("/images/")
                        || uri.startsWith("/uploads/")) {

                    return true;
                }

                // Lấy session hiện tại
                HttpSession session = request.getSession(false);

                // Chưa đăng nhập
                if (session == null ||
                        session.getAttribute("loggedInUser") == null) {

                    response.sendRedirect("/");
                    return false;
                }

                User user = (User) session.getAttribute("loggedInUser");

                // Chỉ tài khoản ACTIVE mới được sử dụng
                if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
                    session.invalidate();
                    response.sendRedirect("/");
                    return false;
                }

                // Chỉ ADMIN được truy cập /admin/**
                if (uri.startsWith("/admin")
                        && !"ADMIN".equalsIgnoreCase(user.getRole())) {

                    response.sendError(
                            HttpServletResponse.SC_FORBIDDEN,
                            "Bạn không có quyền truy cập!"
                    );

                    return false;
                }

                return true;
            }
        });
    }
}
