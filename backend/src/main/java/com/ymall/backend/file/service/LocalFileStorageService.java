package com.ymall.backend.file.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import net.coobird.thumbnailator.Thumbnails;

import com.ymall.backend.file.domain.FilePurpose;
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
    private static final Map<String, byte[]> FILE_SIGNATURES = Map.of(
        "image/jpeg", new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
        "image/png", new byte[] {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
        }
    );
    private static final String THUMBNAIL_PREFIX = "thumb-";
    private static final String THUMBNAIL_EXTENSION = "jpg";
    private static final DateTimeFormatter DATE_DIRECTORY_FORMAT =
        DateTimeFormatter.ofPattern("uuuu/MM/dd");

    private final FileStorageProperties fileStorageProperties;

    public LocalFileStorageService(FileStorageProperties fileStorageProperties) {
        this.fileStorageProperties = fileStorageProperties;
    }

    /**
     * 업로드 이미지를 용도와 UTC 날짜 기준 디렉터리에 저장한다.
     * WebP는 기본 ImageIO 환경의 제약 때문에 원본을 썸네일 경로에도 복사한다.
     */
    @Override
    public FileUploadResponse storeImage(MultipartFile file, FilePurpose purpose) {
        String contentType = validateImage(file);

        try {
            LocalDate uploadDate = LocalDate.now(ZoneOffset.UTC);
            String originalFileName = cleanFileName(file.getOriginalFilename());
            String extension = extensionFor(contentType);
            String storedFileName = UUID.randomUUID() + "." + extension;
            String thumbnailExtension = "image/webp".equals(contentType)
                ? "webp"
                : THUMBNAIL_EXTENSION;
            String thumbnailFileName = THUMBNAIL_PREFIX
                + storedFileName.replace("." + extension, "." + thumbnailExtension);
            Path imageDirectory = createImageDirectory(purpose, uploadDate);
            Path imagePath = imageDirectory.resolve(storedFileName);
            Path thumbnailPath = imageDirectory.resolve(thumbnailFileName);

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, imagePath, StandardCopyOption.REPLACE_EXISTING);
            }

            try {
                if ("image/webp".equals(contentType)) {
                    Files.copy(imagePath, thumbnailPath, StandardCopyOption.REPLACE_EXISTING);
                } else {
                    Thumbnails.of(imagePath.toFile())
                        .size(
                            fileStorageProperties.thumbnailWidth(),
                            fileStorageProperties.thumbnailHeight()
                        )
                        .outputFormat(THUMBNAIL_EXTENSION)
                        .toFile(thumbnailPath.toFile());
                }
            } catch (IOException exception) {
                Files.deleteIfExists(imagePath);
                Files.deleteIfExists(thumbnailPath);
                throw exception;
            }

            return new FileUploadResponse(
                originalFileName,
                storedFileName,
                buildFileUrl(purpose, uploadDate, storedFileName),
                thumbnailFileName,
                buildFileUrl(purpose, uploadDate, thumbnailFileName),
                file.getSize(),
                contentType
            );
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, exception);
        }
    }

    private String validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_EMPTY);
        }

        String contentType = file.getContentType();
        if (contentType == null) {
            throw new BusinessException(ErrorCode.INVALID_IMAGE_TYPE);
        }
        String normalizedContentType = contentType.toLowerCase(Locale.ROOT);
        if (!ALLOWED_CONTENT_TYPES.contains(normalizedContentType)
            || !hasExpectedSignature(file, normalizedContentType)) {
            throw new BusinessException(ErrorCode.INVALID_IMAGE_TYPE);
        }
        return normalizedContentType;
    }

    private boolean hasExpectedSignature(MultipartFile file, String contentType) {
        try (InputStream inputStream = file.getInputStream()) {
            byte[] header = inputStream.readNBytes(12);
            if ("image/webp".equals(contentType)) {
                return header.length >= 12
                    && Arrays.equals(
                        Arrays.copyOfRange(header, 0, 4),
                        "RIFF".getBytes(StandardCharsets.US_ASCII)
                    )
                    && Arrays.equals(
                        Arrays.copyOfRange(header, 8, 12),
                        "WEBP".getBytes(StandardCharsets.US_ASCII)
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

    private Path createImageDirectory(FilePurpose purpose, LocalDate uploadDate) throws IOException {
        Path imageDirectory = Path.of(
                fileStorageProperties.uploadDir(),
                purpose.storageDirectory(),
                uploadDate.format(DATE_DIRECTORY_FORMAT)
            )
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

    private String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> "jpg";
        };
    }

    private String buildFileUrl(FilePurpose purpose, LocalDate uploadDate, String fileName) {
        if (!purpose.isPubliclyAccessible()) {
            return null;
        }
        return fileStorageProperties.imageUrlPrefix()
            + "/"
            + purpose.storageDirectory()
            + "/"
            + uploadDate.format(DATE_DIRECTORY_FORMAT)
            + "/"
            + fileName;
    }
}
