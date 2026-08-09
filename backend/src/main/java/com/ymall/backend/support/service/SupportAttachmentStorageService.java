package com.ymall.backend.support.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.ymall.backend.global.config.FileStorageProperties;
import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.global.util.MultipartFileUtils;

@Service
@RequiredArgsConstructor
public class SupportAttachmentStorageService {

    public static final int MAX_FILES_PER_MESSAGE = 5;
    public static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    public static final long MAX_TOTAL_SIZE = 10L * 1024 * 1024;

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
        "image/jpeg", "image/png", "image/webp", "application/pdf"
    );
    private static final DateTimeFormatter DATE_DIRECTORY_FORMAT =
        DateTimeFormatter.ofPattern("uuuu/MM/dd");

    private final FileStorageProperties fileStorageProperties;

    public void validateAll(List<MultipartFile> files) {
        files.forEach(this::validate);
    }

    public StoredSupportAttachment store(MultipartFile file) {
        String contentType = validate(file);
        String extension = extensionFor(contentType);
        String storedFileName = UUID.randomUUID() + "." + extension;
        String relativePath = LocalDate.now(ZoneOffset.UTC).format(DATE_DIRECTORY_FORMAT)
            + "/" + storedFileName;
        Path target = supportRoot().resolve(relativePath).normalize();
        if (!target.startsWith(supportRoot())) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
        try {
            Files.createDirectories(target.getParent());
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return new StoredSupportAttachment(
                MultipartFileUtils.sanitizeOriginalFileName(
                    file.getOriginalFilename(),
                    "attachment"
                ),
                relativePath.replace('\\', '/'),
                contentType,
                file.getSize()
            );
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, exception);
        }
    }

    public Resource load(String storedPath) {
        Path target = supportRoot().resolve(storedPath).normalize();
        if (!target.startsWith(supportRoot()) || !Files.isRegularFile(target)) {
            throw new BusinessException(ErrorCode.SUPPORT_ATTACHMENT_NOT_FOUND);
        }
        try {
            return new UrlResource(target.toUri());
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.SUPPORT_ATTACHMENT_NOT_FOUND, exception);
        }
    }

    private String validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_EMPTY);
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.FILE_SIZE_EXCEEDED);
        }
        return MultipartFileUtils.validateContentType(
            file,
            ALLOWED_CONTENT_TYPES,
            ErrorCode.INVALID_ATTACHMENT_TYPE
        );
    }

    private Path supportRoot() {
        return Path.of(fileStorageProperties.uploadDir(), "private", "support")
            .toAbsolutePath()
            .normalize();
    }

    private String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "application/pdf" -> "pdf";
            default -> "jpg";
        };
    }
}
