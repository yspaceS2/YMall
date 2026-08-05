package com.ymall.backend.support.service;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.global.security.MemberPrincipal;
import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.support.entity.SupportAttachment;
import com.ymall.backend.support.entity.SupportInquiry;
import com.ymall.backend.support.entity.SupportMessage;
import com.ymall.backend.support.repository.SupportAttachmentRepository;

@Service
@RequiredArgsConstructor
class SupportAttachmentService {

    private final SupportAttachmentRepository attachmentRepository;
    private final SupportAttachmentStorageService attachmentStorageService;

    List<MultipartFile> validateFiles(List<MultipartFile> files) {
        List<MultipartFile> safeFiles = files == null ? List.of() : files;
        if (safeFiles.isEmpty()
            || safeFiles.size() > SupportAttachmentStorageService.MAX_FILES_PER_MESSAGE) {
            throw new BusinessException(
                safeFiles.isEmpty()
                    ? ErrorCode.FILE_EMPTY
                    : ErrorCode.SUPPORT_ATTACHMENT_COUNT_EXCEEDED
            );
        }
        if (safeFiles.stream().mapToLong(MultipartFile::getSize).sum()
            > SupportAttachmentStorageService.MAX_TOTAL_SIZE) {
            throw new BusinessException(ErrorCode.FILE_SIZE_EXCEEDED);
        }
        attachmentStorageService.validateAll(safeFiles);
        return safeFiles;
    }

    void storeAll(SupportMessage message, List<MultipartFile> files) {
        files.stream()
            .map(attachmentStorageService::store)
            .map(stored -> attachmentRepository.save(new SupportAttachment(
                message,
                stored.originalFileName(),
                stored.storedPath(),
                stored.contentType(),
                stored.fileSize()
            )))
            .forEach(message.getAttachments()::add);
    }

    SupportAttachment getAccessibleAttachment(MemberPrincipal principal, Long attachmentId) {
        SupportAttachment attachment = attachmentRepository.findById(attachmentId)
            .orElseThrow(() -> new BusinessException(ErrorCode.SUPPORT_ATTACHMENT_NOT_FOUND));
        SupportInquiry inquiry = attachment.getMessage().getInquiry();
        if (principal.role() != MemberRole.ROLE_ADMIN
            && !inquiry.getMember().getId().equals(principal.memberId())) {
            throw new BusinessException(ErrorCode.SUPPORT_ATTACHMENT_NOT_FOUND);
        }
        return attachment;
    }

    Resource load(SupportAttachment attachment) {
        return attachmentStorageService.load(attachment.getStoredPath());
    }
}
