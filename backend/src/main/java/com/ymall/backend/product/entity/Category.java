package com.ymall.backend.product.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "categories")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    // URL용 name
    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @Column(nullable = false)
    private int depth = 1;

    @Column(nullable = false)
    private int displayOrder = 0;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public Category(String name, String slug) {
        this.name = name;
        this.slug = slug;
    }

    public Category(
        String name,
        String slug,
        Category parent,
        int depth,
        int displayOrder,
        boolean active
    ) {
        this.name = name;
        this.slug = slug;
        this.parent = parent;
        this.depth = depth;
        this.displayOrder = displayOrder;
        this.active = active;
    }

    public void update(
        String name,
        String slug,
        Category parent,
        int depth,
        int displayOrder,
        boolean active
    ) {
        this.name = name;
        this.slug = slug;
        this.parent = parent;
        this.depth = depth;
        this.displayOrder = displayOrder;
        this.active = active;
    }

    public void changeDepth(int depth) {
        this.depth = depth;
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
