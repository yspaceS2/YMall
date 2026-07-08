package com.ymall.backend.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ymall.backend.product.entity.ProductImage;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
}
