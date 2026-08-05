package com.ymall.backend.global.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;

class MultipartFileUtilsTest {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
        "image/jpeg",
        "image/png",
        "image/webp",
        "application/pdf"
    );

    @Test
    void validatesAndNormalizesAllowedContentType() {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "document.pdf",
            "APPLICATION/PDF",
            "%PDF-1.7".getBytes(StandardCharsets.US_ASCII)
        );

        assertThat(MultipartFileUtils.validateContentType(
            file,
            ALLOWED_CONTENT_TYPES,
            ErrorCode.INVALID_ATTACHMENT_TYPE
        )).isEqualTo("application/pdf");
    }

    @Test
    void rejectsContentWhoseSignatureDoesNotMatchMimeType() {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "fake.jpg",
            "image/jpeg",
            "not-an-image".getBytes(StandardCharsets.US_ASCII)
        );

        assertThatThrownBy(() -> MultipartFileUtils.validateContentType(
            file,
            ALLOWED_CONTENT_TYPES,
            ErrorCode.INVALID_ATTACHMENT_TYPE
        ))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INVALID_ATTACHMENT_TYPE);
    }

    @Test
    void wrapsSignatureReadFailureAsFileUploadFailure() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        given(file.isEmpty()).willReturn(false);
        given(file.getContentType()).willReturn("image/jpeg");
        given(file.getInputStream()).willThrow(new IOException("read failure"));

        assertThatThrownBy(() -> MultipartFileUtils.validateContentType(
            file,
            ALLOWED_CONTENT_TYPES,
            ErrorCode.INVALID_ATTACHMENT_TYPE
        ))
            .isInstanceOf(BusinessException.class)
            .hasCauseInstanceOf(IOException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.FILE_UPLOAD_FAILED);
    }

    @Test
    void sanitizesPathAndControlCharactersFromOriginalFileName() {
        assertThat(MultipartFileUtils.sanitizeOriginalFileName(
            "../folder\\unsafe\u0000name.png",
            "attachment"
        )).isEqualTo("unsafename.png");
    }

    @Test
    void usesFallbackWhenOriginalFileNameIsMissing() {
        assertThat(MultipartFileUtils.sanitizeOriginalFileName(null, "attachment"))
            .isEqualTo("attachment");
    }
}
