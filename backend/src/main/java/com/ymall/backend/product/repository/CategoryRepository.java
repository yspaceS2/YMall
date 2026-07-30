package com.ymall.backend.product.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ymall.backend.product.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findBySlug(String slug);

    boolean existsBySlugIgnoreCase(String slug);

    boolean existsBySlugIgnoreCaseAndIdNot(String slug, Long categoryId);

    boolean existsByParentId(Long parentId);

    @EntityGraph(attributePaths = "parent")
    List<Category> findAllByOrderByDepthAscDisplayOrderAscNameAsc();

    @EntityGraph(attributePaths = "parent")
    List<Category> findByActiveTrue(Sort sort);

    @EntityGraph(attributePaths = "parent")
    List<Category> findByParentIdOrderByDisplayOrderAscNameAsc(Long parentId);

    @EntityGraph(attributePaths = "parent")
    @Query("""
        select category from Category category
        where lower(category.name) like lower(concat('%', :keyword, '%'))
           or lower(category.slug) like lower(concat('%', :keyword, '%'))
        order by category.depth, category.displayOrder, category.name
        """)
    List<Category> search(@Param("keyword") String keyword);
}
