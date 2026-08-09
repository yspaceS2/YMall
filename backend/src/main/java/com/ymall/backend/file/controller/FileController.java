package com.ymall.backend.file.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

import com.ymall.backend.file.domain.FilePurpose;
import com.ymall.backend.file.dto.FileUploadResponse;
import com.ymall.backend.file.service.FileStorageService;
import com.ymall.backend.global.common.ApiResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/files")
public class FileController {

    private final FileStorageService fileStorageService;

    @PostMapping("/images")
    public ApiResponse<FileUploadResponse> uploadImage(@RequestParam("file") MultipartFile file) {
        return ApiResponse.success(
            fileStorageService.storeImage(file, FilePurpose.PRODUCT_IMAGE),
            "이미지가 업로드되었습니다."
        );
    }
}
