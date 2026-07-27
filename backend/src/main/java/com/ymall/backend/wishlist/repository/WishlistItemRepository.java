package com.ymall.backend.wishlist.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ymall.backend.product.entity.ProductStatus;
import com.ymall.backend.wishlist.entity.WishlistItem;

public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {

    boolean existsByMemberIdAndProductId(Long memberId, Long productId);

    @EntityGraph(attributePaths = {"product", "product.category"})
    @Query("""
        select item
        from WishlistItem item
        where item.member.id = :memberId
          and item.product.status <> :excludedStatus
        """)
    Page<WishlistItem> findVisibleByMemberId(
        @Param("memberId") Long memberId,
        @Param("excludedStatus") ProductStatus excludedStatus,
        Pageable pageable
    );

    @Modifying
    long deleteByMemberIdAndProductId(Long memberId, Long productId);
}
