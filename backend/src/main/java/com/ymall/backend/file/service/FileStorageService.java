package com.ymall.backend.file.service;

import org.springframework.web.multipart.MultipartFile;

import com.ymall.backend.file.domain.FilePurpose;
import com.ymall.backend.file.dto.FileUploadResponse;

/**
 * 업로드 파일을 검증하고 서비스가 관리하는 이름과 위치로 저장하는 계약이다.
 * 원본 파일명이나 클라이언트 Content-Type만 신뢰해서는 안 된다.
 */
public interface FileStorageService {

    /**
     * 용도별 공개 정책을 적용해 이미지를 저장한다.
     *
     * @param file 검증할 Multipart 이미지
     * @param purpose 저장 경로와 공개 URL 허용 여부를 결정하는 용도
     * @return 무작위 저장 이름과 공개 가능한 경우에만 URL을 포함한 결과
     */
    FileUploadResponse storeImage(MultipartFile file, FilePurpose purpose);
}
