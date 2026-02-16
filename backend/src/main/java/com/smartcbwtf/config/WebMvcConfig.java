package com.smartcbwtf.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Configure serving of static files and global interceptors.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

        @Value("${app.upload.profile-photos:uploads/profiles}")
        private String profilePhotosDir;

        private final FeatureGateInterceptor featureGateInterceptor;

        public WebMvcConfig(FeatureGateInterceptor featureGateInterceptor) {
                this.featureGateInterceptor = featureGateInterceptor;
        }

        @Override
        public void addResourceHandlers(ResourceHandlerRegistry registry) {
                // Serve profile photos from the uploads directory
                String profilePhotosAbsPath = Paths.get(profilePhotosDir).toAbsolutePath().toString();
                registry.addResourceHandler("/uploads/profiles/**")
                                .addResourceLocations("file:" + profilePhotosAbsPath + "/");

                // Serve payment QR codes from the uploads directory
                String paymentQrAbsPath = Paths.get("uploads/payment-qr").toAbsolutePath().toString();
                registry.addResourceHandler("/uploads/payment-qr/**")
                                .addResourceLocations("file:" + paymentQrAbsPath + "/");

                // Serve branding assets (logos) from the uploads directory
                String brandingAbsPath = Paths.get("uploads/branding").toAbsolutePath().toString();
                registry.addResourceHandler("/uploads/branding/**")
                                .addResourceLocations("file:" + brandingAbsPath + "/");

                // Serve generated files (PDFs) from the files directory
                String filesAbsPath = Paths.get("files").toAbsolutePath().toString();
                registry.addResourceHandler("/files/**")
                                .addResourceLocations("file:" + filesAbsPath + "/");
        }

        @Override
        public void addInterceptors(InterceptorRegistry registry) {
                // Register feature gate interceptor for API endpoints
                registry.addInterceptor(featureGateInterceptor)
                                .addPathPatterns("/api/**")
                                .excludePathPatterns(
                                                "/api/auth/**", // Auth endpoints
                                                "/api/config/**", // Config endpoints (mobile uses this)
                                                "/api/health/**", // Health checks
                                                "/api/errors/**" // Error reporting
                                );
        }
}
