package com.ymall.backend.file.dto;

public record FileUploadResponse(
    String originalFileName,
    String storedFileName,
    String fileUrl,
    String thumbnailFileName,
    String thumbnailUrl,
    long size,
    String contentType
) {
}
