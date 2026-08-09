package com.ymall.backend.file.service;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import net.coobird.thumbnailator.Thumbnails;

import com.ymall.backend.file.domain.FilePurpose;
import com.ymall.backend.file.dto.FileUploadResponse;
import com.ymall.backend.global.config.FileStorageProperties;
import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.global.util.MultipartFileUtils;

@Service
public class LocalFileStorageService implements FileStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
        "image/jpeg",
        "image/png",
        "image/webp"
    );
    private static final String THUMBNAIL_PREFIX = "thumb-";
    private static final String THUMBNAIL_EXTENSION = "jpg";
    private static final int MAX_IMAGE_DIMENSION = 20_000;
    private static final long MAX_IMAGE_PIXELS = 40_000_000L;
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
            String originalFileName = MultipartFileUtils.sanitizeOriginalFileName(
                file.getOriginalFilename(),
                "image"
            );
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
        String contentType = MultipartFileUtils.validateContentType(
            file,
            ALLOWED_CONTENT_TYPES,
            ErrorCode.INVALID_IMAGE_TYPE
        );
        validateDecodableImage(file, contentType);
        return contentType;
    }

    private void validateDecodableImage(MultipartFile file, String contentType) {
        if ("image/webp".equals(contentType)) {
            validateWebpContainer(file);
            return;
        }

        try (
            InputStream inputStream = file.getInputStream();
            ImageInputStream imageInputStream = ImageIO.createImageInputStream(inputStream)
        ) {
            if (imageInputStream == null) {
                throw new BusinessException(ErrorCode.INVALID_IMAGE_TYPE);
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInputStream);
            if (!readers.hasNext()) {
                throw new BusinessException(ErrorCode.INVALID_IMAGE_TYPE);
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(imageInputStream, true, true);
                if (exceedsImageLimits(reader.getWidth(0), reader.getHeight(0))) {
                    throw new BusinessException(ErrorCode.INVALID_IMAGE_TYPE);
                }
                BufferedImage image = reader.read(0);
                if (image == null) {
                    throw new BusinessException(ErrorCode.INVALID_IMAGE_TYPE);
                }
            } finally {
                reader.dispose();
            }
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.INVALID_IMAGE_TYPE, exception);
        }
    }

    private void validateWebpContainer(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            byte[] header = inputStream.readNBytes(30);
            boolean validChunkType = header.length == 30
                && header[12] == 'V'
                && header[13] == 'P'
                && header[14] == '8'
                && (header[15] == ' ' || header[15] == 'L' || header[15] == 'X');
            long declaredSize = header.length < 8 ? -1 : readUnsignedLittleEndian(header, 4);
            long firstChunkSize = header.length < 20 ? -1 : readUnsignedLittleEndian(header, 16);
            long paddedChunkSize = firstChunkSize + (firstChunkSize % 2L);
            if (!validChunkType
                || declaredSize + 8L != file.getSize()
                || firstChunkSize <= 0
                || paddedChunkSize > file.getSize() - 20L) {
                throw new BusinessException(ErrorCode.INVALID_IMAGE_TYPE);
            }
            int[] dimensions = readWebpDimensions(header, firstChunkSize);
            if (exceedsImageLimits(dimensions[0], dimensions[1])) {
                throw new BusinessException(ErrorCode.INVALID_IMAGE_TYPE);
            }
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.INVALID_IMAGE_TYPE, exception);
        }
    }

    private int[] readWebpDimensions(byte[] header, long firstChunkSize) {
        if (header[15] == 'X' && firstChunkSize >= 10) {
            return new int[] {
                readUnsigned24LittleEndian(header, 24) + 1,
                readUnsigned24LittleEndian(header, 27) + 1
            };
        }
        if (header[15] == 'L' && firstChunkSize >= 5 && header[20] == 0x2F) {
            int width = 1 + ((header[21] & 0xFF) | ((header[22] & 0x3F) << 8));
            int height = 1
                + (((header[22] & 0xC0) >> 6)
                    | ((header[23] & 0xFF) << 2)
                    | ((header[24] & 0x0F) << 10));
            return new int[] {width, height};
        }
        if (header[15] == ' '
            && firstChunkSize >= 10
            && (header[23] & 0xFF) == 0x9D
            && (header[24] & 0xFF) == 0x01
            && (header[25] & 0xFF) == 0x2A) {
            int width = ((header[26] & 0xFF) | ((header[27] & 0xFF) << 8)) & 0x3FFF;
            int height = ((header[28] & 0xFF) | ((header[29] & 0xFF) << 8)) & 0x3FFF;
            return new int[] {width, height};
        }
        throw new BusinessException(ErrorCode.INVALID_IMAGE_TYPE);
    }

    private int readUnsigned24LittleEndian(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFF)
            | ((bytes[offset + 1] & 0xFF) << 8)
            | ((bytes[offset + 2] & 0xFF) << 16);
    }

    private long readUnsignedLittleEndian(byte[] bytes, int offset) {
        return Integer.toUnsignedLong(
            (bytes[offset] & 0xFF)
                | ((bytes[offset + 1] & 0xFF) << 8)
                | ((bytes[offset + 2] & 0xFF) << 16)
                | ((bytes[offset + 3] & 0xFF) << 24)
        );
    }

    private boolean exceedsImageLimits(int width, int height) {
        return width <= 0
            || height <= 0
            || width > MAX_IMAGE_DIMENSION
            || height > MAX_IMAGE_DIMENSION
            || (long) width * height > MAX_IMAGE_PIXELS;
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
