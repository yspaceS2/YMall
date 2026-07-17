package com.ymall.backend.product.entity;

import java.math.BigDecimal;
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

import com.ymall.backend.seller.entity.SellerProfile;

@Getter
@Entity
@Table(name = "products")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_profile_id")
    private SellerProfile sellerProfile;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 100)
    private String brand;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(precision = 5, scale = 2)
    private BigDecimal discountPercentage;

    @Column(precision = 3, scale = 2)
    private BigDecimal rating;

    @Column(nullable = false)
    private Integer stock;

    @Column(columnDefinition = "TEXT")
    private String thumbnailUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProductStatus status;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductImage> images = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public Product(
        Category category,
        String name,
        String description,
        String brand,
        BigDecimal price,
        BigDecimal discountPercentage,
        BigDecimal rating,
        Integer stock,
        String thumbnailUrl,
        ProductStatus status
    ) {
        this.category = category;
        this.name = name;
        this.description = description;
        this.brand = brand;
        this.price = price;
        this.discountPercentage = discountPercentage;
        this.rating = rating;
        this.stock = stock;
        this.thumbnailUrl = thumbnailUrl;
        this.status = status;
    }

    public void addImage(ProductImage image) {
        images.add(image);
        image.assignProduct(this);
    }

    public void assignSellerProfile(SellerProfile sellerProfile) {
        this.sellerProfile = sellerProfile;
    }

    public void requestApproval() {
        this.status = ProductStatus.PENDING;
    }

    /**
     * 상품의 기본 정보만 수정한다.
     * 평점은 리뷰 도메인에서 집계되어야 하므로 상품 수정 요청으로 직접 변경하지 않는다.
     */
    public void update(
        Category category,
        String name,
        String description,
        String brand,
        BigDecimal price,
        BigDecimal discountPercentage,
        Integer stock,
        String thumbnailUrl
    ) {
        this.category = category;
        this.name = name;
        this.description = description;
        this.brand = brand;
        this.price = price;
        this.discountPercentage = discountPercentage;
        this.stock = stock;
        this.thumbnailUrl = thumbnailUrl;
    }

    /**
     * 이미지 수정은 요청 이미지 목록 전체를 기준으로 교체한다.
     * orphanRemoval=true 설정으로 기존 이미지 엔티티는 연관관계에서 제거되면 삭제 대상이 된다.
     */
    public void replaceImages(List<ProductImage> newImages) {
        images.clear();
        newImages.forEach(this::addImage);
    }

    /**
     * 상품 삭제는 참조 이력 보존을 위해 상태만 변경한다.
     * 공개 조회에서는 DELETED 상태를 제외한다.
     */
    public void delete() {
        this.status = ProductStatus.DELETED;
    }

    public void decreaseStock(int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("차감 수량은 1개 이상이어야 합니다.");
        }
        if (stock < quantity) {
            throw new IllegalArgumentException("상품 재고보다 많은 수량을 차감할 수 없습니다.");
        }

        this.stock -= quantity;
    }

    public void increaseStock(int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("복구 수량은 1개 이상이어야 합니다.");
        }

        long restoredStock = (long) stock + quantity;
        if (restoredStock > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("상품 재고가 허용 범위를 초과합니다.");
        }

        this.stock = (int) restoredStock;
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
