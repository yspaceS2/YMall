package com.ymall.backend.file.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import net.coobird.thumbnailator.Thumbnails;

import com.ymall.backend.file.dto.FileUploadResponse;
import com.ymall.backend.global.config.FileStorageProperties;
import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;

@Service
public class LocalFileStorageService implements FileStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
        "image/jpeg",
        "image/png",
        "image/webp"
    );
    private static final String IMAGE_DIRECTORY = "products";
    private static final String THUMBNAIL_PREFIX = "thumb-";
    private static final String THUMBNAIL_EXTENSION = "jpg";

    private final FileStorageProperties fileStorageProperties;

    public LocalFileStorageService(FileStorageProperties fileStorageProperties) {
        this.fileStorageProperties = fileStorageProperties;
    }

    /**
     * 업로드 이미지를 로컬 디렉터리에 저장하고, 같은 위치에 JPG 썸네일을 생성한다.
     * WebP 출력은 Java 기본 ImageIO 환경에서 제약이 있어 썸네일은 JPG로 통일한다.
     */
    @Override
    public FileUploadResponse storeImage(MultipartFile file) throws IOException {
        validateImage(file);

        String originalFileName = cleanFileName(file.getOriginalFilename());
        String extension = extractExtension(originalFileName);
        String storedFileName = UUID.randomUUID() + "." + extension;
        String thumbnailFileName = THUMBNAIL_PREFIX + storedFileName.replace("." + extension, "." + THUMBNAIL_EXTENSION);
        Path imageDirectory = createImageDirectory();
        Path imagePath = imageDirectory.resolve(storedFileName);
        Path thumbnailPath = imageDirectory.resolve(thumbnailFileName);

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, imagePath, StandardCopyOption.REPLACE_EXISTING);
        }

        Thumbnails.of(imagePath.toFile())
            .size(fileStorageProperties.thumbnailWidth(), fileStorageProperties.thumbnailHeight())
            .outputFormat(THUMBNAIL_EXTENSION)
            .toFile(thumbnailPath.toFile());

        return new FileUploadResponse(
            originalFileName,
            storedFileName,
            buildFileUrl(storedFileName),
            thumbnailFileName,
            buildFileUrl(thumbnailFileName),
            file.getSize(),
            file.getContentType()
        );
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_EMPTY);
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BusinessException(ErrorCode.INVALID_IMAGE_TYPE);
        }
    }

    private Path createImageDirectory() throws IOException {
        Path imageDirectory = Path.of(fileStorageProperties.uploadDir(), IMAGE_DIRECTORY)
            .toAbsolutePath()
            .normalize();

        Files.createDirectories(imageDirectory);

        return imageDirectory;
    }

    private String cleanFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            return "image";
        }

        return Path.of(originalFileName).getFileName().toString();
    }

    private String extractExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf(".");
        if (lastDotIndex < 0 || lastDotIndex == fileName.length() - 1) {
            return "jpg";
        }

        return fileName.substring(lastDotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private String buildFileUrl(String fileName) {
        return fileStorageProperties.imageUrlPrefix() + "/" + IMAGE_DIRECTORY + "/" + fileName;
    }
}
