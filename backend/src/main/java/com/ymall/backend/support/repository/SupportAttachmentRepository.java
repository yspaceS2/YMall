package com.ymall.backend.support.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ymall.backend.support.entity.SupportAttachment;

public interface SupportAttachmentRepository extends JpaRepository<SupportAttachment, Long> {

    @Override
    @EntityGraph(attributePaths = {"message", "message.inquiry", "message.inquiry.member"})
    java.util.Optional<SupportAttachment> findById(Long attachmentId);
}
