package com.ymall.backend.productquestion.entity;

import java.time.LocalDateTime;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.ymall.backend.member.entity.Member;
import com.ymall.backend.product.entity.Product;

@Getter
@Entity
@Table(name = "product_questions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "question_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_private", nullable = false)
    private boolean privateQuestion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductQuestionStatus status;

    @OneToOne(mappedBy = "question", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private ProductQuestionAnswer answer;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public ProductQuestion(
        Product product,
        Member member,
        String title,
        String content,
        boolean privateQuestion
    ) {
        this.product = product;
        this.member = member;
        this.title = title;
        this.content = content;
        this.privateQuestion = privateQuestion;
        this.status = ProductQuestionStatus.WAITING;
    }

    public void update(String title, String content, boolean privateQuestion) {
        this.title = title;
        this.content = content;
        this.privateQuestion = privateQuestion;
    }

    public void attachAnswer(ProductQuestionAnswer answer) {
        this.answer = answer;
        this.status = ProductQuestionStatus.ANSWERED;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
