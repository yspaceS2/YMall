package com.ymall.backend.product.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "product_revisions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_revision_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 100)
    private String brand;

    @Column(columnDefinition = "TEXT")
    private String thumbnailUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProductStatus status;

    @Column(length = 500)
    private String rejectionReason;

    @OneToMany(mappedBy = "revision", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductRevisionImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "revision", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductRevisionDetailImage> detailImages = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime reviewedAt;

    public ProductRevision(
        Product product,
        Category category,
        String name,
        String description,
        String brand,
        String thumbnailUrl
    ) {
        this.product = product;
        update(category, name, description, brand, thumbnailUrl);
        this.status = ProductStatus.PENDING;
    }

    public void update(
        Category category,
        String name,
        String description,
        String brand,
        String thumbnailUrl
    ) {
        this.category = category;
        this.name = name;
        this.description = description;
        this.brand = brand;
        this.thumbnailUrl = thumbnailUrl;
        this.status = ProductStatus.PENDING;
        this.rejectionReason = null;
        this.reviewedAt = null;
    }

    public void replaceImages(List<ProductRevisionImage> newImages) {
        images.clear();
        newImages.forEach(image -> {
            images.add(image);
            image.assignRevision(this);
        });
    }

    public void replaceDetailImages(List<ProductRevisionDetailImage> newImages) {
        detailImages.clear();
        newImages.forEach(image -> {
            detailImages.add(image);
            image.assignRevision(this);
        });
    }

    public void approve() {
        status = ProductStatus.APPROVED;
        rejectionReason = null;
        reviewedAt = LocalDateTime.now();
    }

    public void reject(String reason) {
        status = ProductStatus.REJECTED;
        rejectionReason = reason;
        reviewedAt = LocalDateTime.now();
    }

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
