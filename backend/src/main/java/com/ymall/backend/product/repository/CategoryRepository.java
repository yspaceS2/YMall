package com.ymall.backend.product.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ymall.backend.product.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findBySlug(String slug);
}
