package com.ymall.backend.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.order.entity.Order;
import com.ymall.backend.order.entity.OrderItem;
import com.ymall.backend.product.entity.Category;
import com.ymall.backend.product.entity.Product;
import com.ymall.backend.product.entity.ProductStatus;
import com.ymall.backend.product.repository.ProductRepository;
import com.ymall.backend.product.service.ProductCacheInvalidator;

@ExtendWith(MockitoExtension.class)
class PaymentInventoryServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private ProductCacheInvalidator productCacheInvalidator;

    private PaymentInventoryService inventoryService;
    private Product product;
    private Order order;

    @BeforeEach
    void setUp() {
        inventoryService = new PaymentInventoryService(
            productRepository,
            productCacheInvalidator
        );
        product = new Product(
            new Category("Inventory", "inventory"),
            "Inventory product",
            "description",
            "YMall",
            BigDecimal.valueOf(10000),
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            10,
            null,
            ProductStatus.APPROVED
        );
        ReflectionTestUtils.setField(product, "id", 1L);
        order = new Order(
            new Member("inventory@example.com", "password", "재고 사용자", MemberRole.ROLE_USER),
            "inventory-order"
        );
        order.addItem(new OrderItem(
            product,
            product.getName(),
            BigDecimal.valueOf(10000),
            2
        ));
        given(productRepository.findAllByIdForUpdate(List.of(1L)))
            .willReturn(List.of(product));
    }

    @Test
    void releasesReservedInventoryAndEvictsProductDetail() {
        product.decreaseStock(2);

        inventoryService.releaseIfReserved(order);

        assertThat(product.getStock()).isEqualTo(10);
        assertThat(order.isInventoryReserved()).isFalse();
        then(productCacheInvalidator).should().evictProductDetails(List.of(1L));
    }

    @Test
    void reservesReleasedInventoryAndEvictsProductDetail() {
        order.releaseInventory();

        inventoryService.reserveIfNeeded(order);

        assertThat(product.getStock()).isEqualTo(8);
        assertThat(order.isInventoryReserved()).isTrue();
        then(productCacheInvalidator).should().evictProductDetails(List.of(1L));
    }
}
