package com.ymall.backend.file.service;

import java.io.IOException;

import org.springframework.web.multipart.MultipartFile;

import com.ymall.backend.file.dto.FileUploadResponse;

public interface FileStorageService {

    FileUploadResponse storeImage(MultipartFile file) throws IOException;
}
