package com.ymall.backend.support.service;

public record StoredSupportAttachment(
    String originalFileName,
    String storedPath,
    String contentType,
    long fileSize
) {
}
