package com.ymall.backend.support.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.entity.MemberRole;

@Getter
@Entity
@Table(name = "support_messages")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SupportMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inquiry_id", nullable = false)
    private SupportInquiry inquiry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private Member author;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberRole authorRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SupportMessageType type;

    @Column(nullable = false, length = 2000)
    private String content;

    @OneToMany(mappedBy = "message")
    @OrderBy("id ASC")
    private List<SupportAttachment> attachments = new ArrayList<>();

    private UUID clientMessageId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public SupportMessage(
        SupportInquiry inquiry,
        Member author,
        SupportMessageType type,
        String content,
        UUID clientMessageId
    ) {
        this.inquiry = inquiry;
        this.author = author;
        this.authorRole = author.getRole();
        this.type = type;
        this.content = content;
        this.clientMessageId = clientMessageId;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
