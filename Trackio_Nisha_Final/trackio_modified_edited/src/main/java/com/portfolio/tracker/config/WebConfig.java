package com.portfolio.tracker.config;

import com.portfolio.tracker.util.PhotoUploadUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Serves user-uploaded profile photos from the stable on-disk directory
 * (PhotoUploadUtil.UPLOAD_DIR) under the URL path /uploads/**
 *
 * Uses the same UPLOAD_DIR constant as PhotoUploadUtil so the save path
 * and the serve path are always identical.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Use the exact same directory PhotoUploadUtil saves to
        String location = PhotoUploadUtil.UPLOAD_DIR.toUri().toString();
        if (!location.endsWith("/")) location += "/";

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location)
                .setCachePeriod(0);   // no caching during development
    }
}
