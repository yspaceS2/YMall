package com.ymall.backend.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
/*
* 파일 업로드 환경설정
* */
@ConfigurationProperties(prefix = "ymall.file")
public record FileStorageProperties(
    String uploadDir,
    String imageUrlPrefix,
    int thumbnailWidth,
    int thumbnailHeight
) {
}
