package com.ymall.backend.file.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import com.ymall.backend.file.domain.FilePurpose;
import com.ymall.backend.file.dto.FileUploadResponse;
import com.ymall.backend.file.service.FileStorageService;
import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;

@WebMvcTest(FileController.class)
@AutoConfigureMockMvc(addFilters = false)
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FileStorageService fileStorageService;

    /**
     * multipart/form-data 이미지 업로드 요청을 받아 파일 저장 서비스에 위임하고,
     * 원본 URL과 썸네일 URL을 공통 응답으로 반환하는지 검증한다.
     */
    @Test
    @DisplayName("이미지를 업로드한다")
    void uploadImage() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "product.jpg",
            "image/jpeg",
            "image".getBytes()
        );
        FileUploadResponse response = new FileUploadResponse(
            "product.jpg",
            "stored.jpg",
            "/images/products/stored.jpg",
            "thumb-stored.jpg",
            "/images/products/thumb-stored.jpg",
            5,
            "image/jpeg"
        );

        given(fileStorageService.storeImage(
            any(MultipartFile.class),
            any(FilePurpose.class)
        )).willReturn(response);

        mockMvc.perform(multipart("/api/files/images").file(file))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("이미지가 업로드되었습니다."))
            .andExpect(jsonPath("$.data.fileUrl").value("/images/products/stored.jpg"))
            .andExpect(jsonPath("$.data.thumbnailUrl").value("/images/products/thumb-stored.jpg"));
    }

    @Test
    @DisplayName("파일 업로드 실패를 공통 오류 응답으로 반환한다")
    void uploadImageFailure() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "product.jpg",
            "image/jpeg",
            "image".getBytes()
        );

        given(fileStorageService.storeImage(
            any(MultipartFile.class),
            any(FilePurpose.class)
        ))
            .willThrow(new BusinessException(ErrorCode.FILE_UPLOAD_FAILED));

        mockMvc.perform(multipart("/api/files/images").file(file))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("FILE_UPLOAD_FAILED"))
            .andExpect(jsonPath("$.error.message").value("파일 업로드에 실패했습니다."));
    }
}
