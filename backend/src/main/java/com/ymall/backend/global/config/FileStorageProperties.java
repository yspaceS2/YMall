package com.ymall.backend.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ymall.file")
public record FileStorageProperties(
    String uploadDir,
    String imageUrlPrefix,
    int thumbnailWidth,
    int thumbnailHeight
) {
}
