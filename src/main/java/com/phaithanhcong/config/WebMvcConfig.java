package com.phaithanhcong.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

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
}
