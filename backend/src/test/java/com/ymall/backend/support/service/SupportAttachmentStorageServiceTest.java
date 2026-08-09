package com.ymall.backend.support.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.ymall.backend.global.config.FileStorageProperties;
import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;

class SupportAttachmentStorageServiceTest {

    @TempDir
    private Path tempDir;

    @Test
    void storesValidatedAttachmentWithSanitizedFileName() {
        SupportAttachmentStorageService service = createService();
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "../folder\\document.pdf",
            "application/pdf",
            "%PDF-1.7".getBytes(StandardCharsets.US_ASCII)
        );

        StoredSupportAttachment stored = service.store(file);

        assertThat(stored.originalFileName()).isEqualTo("document.pdf");
        assertThat(stored.contentType()).isEqualTo("application/pdf");
        assertThat(stored.storedPath()).endsWith(".pdf");
        assertThat(Files.isRegularFile(
            tempDir.resolve("private/support").resolve(stored.storedPath())
        )).isTrue();
    }

    @Test
    void preservesAttachmentSizeLimitError() {
        SupportAttachmentStorageService service = createService();
        MultipartFile file = mock(MultipartFile.class);
        given(file.isEmpty()).willReturn(false);
        given(file.getSize()).willReturn(SupportAttachmentStorageService.MAX_FILE_SIZE + 1);

        assertThatThrownBy(() -> service.store(file))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.FILE_SIZE_EXCEEDED);
    }

    @Test
    void preservesAttachmentTypeError() {
        SupportAttachmentStorageService service = createService();
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "document.txt",
            "text/plain",
            "text".getBytes(StandardCharsets.UTF_8)
        );

        assertThatThrownBy(() -> service.store(file))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INVALID_ATTACHMENT_TYPE);
    }

    private SupportAttachmentStorageService createService() {
        return new SupportAttachmentStorageService(new FileStorageProperties(
            tempDir.toString(),
            "/images",
            300,
            300
        ));
    }
}
