package com.ymall.backend.support.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ymall.backend.support.entity.SupportMessage;

public interface SupportMessageRepository extends JpaRepository<SupportMessage, Long> {

    @EntityGraph(attributePaths = {"author", "attachments"})
    List<SupportMessage> findByInquiryIdOrderByCreatedAtAscIdAsc(Long inquiryId);

    Optional<SupportMessage> findByInquiryIdAndClientMessageId(Long inquiryId, UUID clientMessageId);

    long countByInquiryId(Long inquiryId);
}
