package com.smartcbwtf.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Configure serving of static files like uploaded profile photos.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.upload.profile-photos:uploads/profiles}")
    private String profilePhotosDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve profile photos from the uploads directory
        String absolutePath = Paths.get(profilePhotosDir).toAbsolutePath().toString();

        registry.addResourceHandler("/uploads/profiles/**")
                .addResourceLocations("file:" + absolutePath + "/");
    }
}
