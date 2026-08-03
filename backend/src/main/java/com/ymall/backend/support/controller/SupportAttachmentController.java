package com.ymall.backend.support.controller;

import java.nio.charset.StandardCharsets;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ymall.backend.global.security.MemberPrincipal;
import com.ymall.backend.support.entity.SupportAttachment;
import com.ymall.backend.support.service.SupportService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/support/attachments")
public class SupportAttachmentController {

    private final SupportService supportService;

    @GetMapping("/{attachmentId}")
    public ResponseEntity<Resource> download(
        @AuthenticationPrincipal MemberPrincipal principal,
        @PathVariable Long attachmentId
    ) {
        SupportAttachment attachment = supportService.getAttachment(principal, attachmentId);
        Resource resource = supportService.loadAttachment(attachment);
        ContentDisposition disposition = ContentDisposition.inline()
            .filename(attachment.getOriginalFileName(), StandardCharsets.UTF_8)
            .build();
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(attachment.getContentType()))
            .contentLength(attachment.getFileSize())
            .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
            .header("X-Content-Type-Options", "nosniff")
            .body(resource);
    }
}
