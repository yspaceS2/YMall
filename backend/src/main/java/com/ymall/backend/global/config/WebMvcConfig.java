package com.ymall.backend.global.config;

import java.nio.file.Path;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(FileStorageProperties.class)
public class WebMvcConfig implements WebMvcConfigurer {

    private final FileStorageProperties fileStorageProperties;

    public WebMvcConfig(FileStorageProperties fileStorageProperties) {
        this.fileStorageProperties = fileStorageProperties;
    }

    /**
     * 로컬에 저장한 업로드 파일을 HTTP URL로 접근할 수 있게 매핑한다.
     * S3로 이전하면 이 매핑 대신 S3 URL을 응답에 저장하는 구조로 교체한다.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String urlPattern = fileStorageProperties.imageUrlPrefix() + "/**";
        String uploadLocation = Path.of(fileStorageProperties.uploadDir()).toAbsolutePath().normalize().toUri().toString();

        registry.addResourceHandler(urlPattern)
            .addResourceLocations(uploadLocation);
    }
}
