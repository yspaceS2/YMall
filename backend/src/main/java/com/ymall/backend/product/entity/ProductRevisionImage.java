package com.ymall.backend.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "product_revision_images")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductRevisionImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_revision_image_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_revision_id", nullable = false)
    private ProductRevision revision;

    @Column(columnDefinition = "TEXT")
    private String originalUrl;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String imageUrl;

    @Column(nullable = false)
    private Integer sortOrder;

    public ProductRevisionImage(String originalUrl, String imageUrl, Integer sortOrder) {
        this.originalUrl = originalUrl;
        this.imageUrl = imageUrl;
        this.sortOrder = sortOrder;
    }

    void assignRevision(ProductRevision revision) {
        this.revision = revision;
    }
}
