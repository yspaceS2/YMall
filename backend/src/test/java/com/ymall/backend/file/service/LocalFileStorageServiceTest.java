package com.ymall.backend.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.ymall.backend.file.domain.FilePurpose;
import com.ymall.backend.file.dto.FileUploadResponse;
import com.ymall.backend.global.config.FileStorageProperties;
import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;

class LocalFileStorageServiceTest {

    @TempDir
    private Path tempDir;

    /**
     * 이미지 파일을 업로드하면 원본 파일과 썸네일 파일을 로컬 저장소에 생성하는지 검증한다.
     * 응답 URL은 프론트에서 바로 사용할 수 있도록 /images/products 경로로 반환한다.
     */
    @Test
    @DisplayName("이미지와 썸네일을 로컬 저장소에 저장한다")
    void storeImage() throws Exception {
        LocalFileStorageService service = createService();
        MockMultipartFile file = createImageFile();

        FileUploadResponse response = service.storeImage(file, FilePurpose.PRODUCT_IMAGE);

        assertThat(response.fileUrl())
            .matches("/images/public/products/\\d{4}/\\d{2}/\\d{2}/.+");
        assertThat(response.thumbnailUrl())
            .matches("/images/public/products/\\d{4}/\\d{2}/\\d{2}/thumb-.+");
        try (var storedFiles = Files.walk(tempDir)) {
            assertThat(storedFiles.map(Path::getFileName))
                .contains(Path.of(response.storedFileName()), Path.of(response.thumbnailFileName()));
        }
    }

    /**
     * 이미지가 아닌 파일은 저장하지 않고 INVALID_IMAGE_TYPE 예외를 발생시키는지 검증한다.
     * 파일 확장자가 아니라 Multipart contentType을 기준으로 1차 검증한다.
     */
    @Test
    @DisplayName("이미지가 아닌 파일 업로드는 거부한다")
    void rejectInvalidContentType() {
        LocalFileStorageService service = createService();
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "text.txt",
            "text/plain",
            "not-image".getBytes()
        );

        assertThatThrownBy(() -> service.storeImage(file, FilePurpose.PRODUCT_IMAGE))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INVALID_IMAGE_TYPE);
    }

    @Test
    @DisplayName("이미지 MIME 타입으로 위장한 파일 업로드는 거절한다")
    void rejectSpoofedImageContent() {
        LocalFileStorageService service = createService();
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "fake.jpg",
            "image/jpeg",
            "not-an-image".getBytes()
        );

        assertThatThrownBy(() -> service.storeImage(file, FilePurpose.PRODUCT_IMAGE))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INVALID_IMAGE_TYPE);
    }

    @Test
    @DisplayName("이미지 서명만 가진 손상된 파일 업로드는 거절한다")
    void rejectCorruptedImageWithValidSignature() {
        LocalFileStorageService service = createService();
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "corrupted.png",
            "image/png",
            new byte[] {
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x00
            }
        );

        assertThatThrownBy(() -> service.storeImage(file, FilePurpose.PRODUCT_IMAGE))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INVALID_IMAGE_TYPE);
    }

    @Test
    @DisplayName("WEBP 컨테이너 크기와 이미지 청크가 유효하지 않으면 거절한다")
    void rejectMalformedWebpContainer() {
        LocalFileStorageService service = createService();
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "fake.webp",
            "image/webp",
            new byte[] {
                'R', 'I', 'F', 'F', 0x08, 0x00, 0x00, 0x00,
                'W', 'E', 'B', 'P', 'J', 'U', 'N', 'K'
            }
        );

        assertThatThrownBy(() -> service.storeImage(file, FilePurpose.PRODUCT_IMAGE))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INVALID_IMAGE_TYPE);
    }

    @Test
    @DisplayName("이미지 데이터가 없는 WEBP 청크는 거절한다")
    void rejectEmptyWebpImageChunk() {
        LocalFileStorageService service = createService();
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "empty.webp",
            "image/webp",
            new byte[] {
                'R', 'I', 'F', 'F', 0x0C, 0x00, 0x00, 0x00,
                'W', 'E', 'B', 'P', 'V', 'P', '8', ' ',
                0x00, 0x00, 0x00, 0x00
            }
        );

        assertThatThrownBy(() -> service.storeImage(file, FilePurpose.PRODUCT_IMAGE))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INVALID_IMAGE_TYPE);
    }

    @Test
    @DisplayName("허용 해상도를 초과하는 WEBP 업로드는 거절한다")
    void rejectOversizedWebpDimensions() {
        LocalFileStorageService service = createService();
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "oversized.webp",
            "image/webp",
            new byte[] {
                'R', 'I', 'F', 'F', 0x16, 0x00, 0x00, 0x00,
                'W', 'E', 'B', 'P', 'V', 'P', '8', 'X',
                0x0A, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00,
                0x20, 0x4E, 0x00,
                0x00, 0x00, 0x00
            }
        );

        assertThatThrownBy(() -> service.storeImage(file, FilePurpose.PRODUCT_IMAGE))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INVALID_IMAGE_TYPE);
    }

    @Test
    @DisplayName("경로가 포함된 원본 파일명은 파일명만 응답한다")
    void sanitizePathTraversalFromOriginalFileName() throws Exception {
        LocalFileStorageService service = createService();
        MockMultipartFile image = createImageFile("../../escaped.jpg");

        FileUploadResponse response = service.storeImage(image, FilePurpose.PRODUCT_IMAGE);

        assertThat(response.originalFileName()).isEqualTo("escaped.jpg");
        assertThat(response.storedFileName()).doesNotContain("..", "/", "\\");
    }

    @Test
    @DisplayName("파일 저장 중 I/O 오류를 공통 비즈니스 예외로 변환한다")
    void wrapIOException() throws Exception {
        LocalFileStorageService service = createService();
        MultipartFile file = mock(MultipartFile.class);

        given(file.isEmpty()).willReturn(false);
        given(file.getContentType()).willReturn("image/jpeg");
        given(file.getOriginalFilename()).willReturn("product.jpg");
        given(file.getInputStream()).willThrow(new IOException("disk error"));

        assertThatThrownBy(() -> service.storeImage(file, FilePurpose.PRODUCT_IMAGE))
            .isInstanceOf(BusinessException.class)
            .hasCauseInstanceOf(IOException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.FILE_UPLOAD_FAILED);
    }

    private LocalFileStorageService createService() {
        return new LocalFileStorageService(
            new FileStorageProperties(
                tempDir.toString(),
                "/images",
                300,
                300
            )
        );
    }

    private MockMultipartFile createImageFile() throws Exception {
        return createImageFile("product.jpg");
    }

    private MockMultipartFile createImageFile(String originalFileName) throws Exception {
        BufferedImage image = new BufferedImage(600, 400, BufferedImage.TYPE_INT_RGB);
        image.getGraphics().setColor(Color.BLUE);
        image.getGraphics().fillRect(0, 0, 600, 400);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", outputStream);

        return new MockMultipartFile(
            "file",
            originalFileName,
            "image/jpeg",
            outputStream.toByteArray()
        );
    }
}
