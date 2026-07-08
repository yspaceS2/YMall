package com.ymall.backend.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

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

        FileUploadResponse response = service.storeImage(file);

        assertThat(response.fileUrl()).startsWith("/images/products/");
        assertThat(response.thumbnailUrl()).startsWith("/images/products/thumb-");
        assertThat(Files.exists(tempDir.resolve("products").resolve(response.storedFileName()))).isTrue();
        assertThat(Files.exists(tempDir.resolve("products").resolve(response.thumbnailFileName()))).isTrue();
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

        assertThatThrownBy(() -> service.storeImage(file))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INVALID_IMAGE_TYPE);
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
