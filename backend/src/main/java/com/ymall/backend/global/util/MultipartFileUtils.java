package com.ymall.backend.global.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.web.multipart.MultipartFile;

import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;

public final class MultipartFileUtils {

    private static final int SIGNATURE_HEADER_LENGTH = 12;
    private static final Map<String, byte[]> FILE_SIGNATURES = Map.of(
        "image/jpeg", new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
        "image/png", new byte[] {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
        },
        "application/pdf", "%PDF-".getBytes(StandardCharsets.US_ASCII)
    );
    private static final byte[] WEBP_RIFF_SIGNATURE =
        "RIFF".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] WEBP_FORMAT_SIGNATURE =
        "WEBP".getBytes(StandardCharsets.US_ASCII);

    private MultipartFileUtils() {
    }

    public static String validateContentType(
        MultipartFile file,
        Set<String> allowedContentTypes,
        ErrorCode invalidTypeErrorCode
    ) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_EMPTY);
        }

        String contentType = file.getContentType();
        if (contentType == null) {
            throw new BusinessException(invalidTypeErrorCode);
        }

        String normalizedContentType = contentType.toLowerCase(Locale.ROOT);
        if (!allowedContentTypes.contains(normalizedContentType)
            || !hasExpectedSignature(file, normalizedContentType)) {
            throw new BusinessException(invalidTypeErrorCode);
        }
        return normalizedContentType;
    }

    public static String sanitizeOriginalFileName(String originalFileName, String fallback) {
        if (originalFileName == null || originalFileName.isBlank()) {
            return fallback;
        }

        String normalized = originalFileName.replace('\\', '/');
        String fileName = normalized.substring(normalized.lastIndexOf('/') + 1)
            .replaceAll("[\\p{Cntrl}]", "")
            .trim();
        if (fileName.isBlank()) {
            return fallback;
        }
        return fileName.length() <= 255 ? fileName : fileName.substring(fileName.length() - 255);
    }

    private static boolean hasExpectedSignature(MultipartFile file, String contentType) {
        try (InputStream inputStream = file.getInputStream()) {
            byte[] header = inputStream.readNBytes(SIGNATURE_HEADER_LENGTH);
            if ("image/webp".equals(contentType)) {
                return header.length >= SIGNATURE_HEADER_LENGTH
                    && Arrays.equals(
                        Arrays.copyOfRange(header, 0, 4),
                        WEBP_RIFF_SIGNATURE
                    )
                    && Arrays.equals(
                        Arrays.copyOfRange(header, 8, 12),
                        WEBP_FORMAT_SIGNATURE
                    );
            }

            byte[] signature = FILE_SIGNATURES.get(contentType);
            return signature != null
                && header.length >= signature.length
                && Arrays.equals(Arrays.copyOf(header, signature.length), signature);
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, exception);
        }
    }
}
