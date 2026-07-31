package com.ymall.backend.file.service;

import org.springframework.web.multipart.MultipartFile;

import com.ymall.backend.file.domain.FilePurpose;
import com.ymall.backend.file.dto.FileUploadResponse;

public interface FileStorageService {

    FileUploadResponse storeImage(MultipartFile file, FilePurpose purpose);
}
