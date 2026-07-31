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
        BufferedImage image = new BufferedImage(600, 400, BufferedImage.TYPE_INT_RGB);
        image.getGraphics().setColor(Color.BLUE);
        image.getGraphics().fillRect(0, 0, 600, 400);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", outputStream);

        return new MockMultipartFile(
            "file",
            "product.jpg",
            "image/jpeg",
            outputStream.toByteArray()
        );
    }
}
