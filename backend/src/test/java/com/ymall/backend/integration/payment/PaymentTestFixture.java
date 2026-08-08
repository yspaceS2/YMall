package com.ymall.backend.integration.payment;

import java.math.BigDecimal;

import com.ymall.backend.cart.entity.CartItem;
import com.ymall.backend.cart.repository.CartItemRepository;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.entity.MemberAddress;
import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.member.repository.MemberAddressRepository;
import com.ymall.backend.member.repository.MemberRepository;
import com.ymall.backend.product.entity.Category;
import com.ymall.backend.product.entity.Product;
import com.ymall.backend.product.entity.ProductStatus;
import com.ymall.backend.product.repository.CategoryRepository;
import com.ymall.backend.product.repository.ProductRepository;

final class PaymentTestFixture {

    private PaymentTestFixture() {
    }

    static PaymentOrderData createOrderData(
        MemberRepository memberRepository,
        MemberAddressRepository memberAddressRepository,
        CategoryRepository categoryRepository,
        ProductRepository productRepository,
        CartItemRepository cartItemRepository,
        String fixtureName
    ) {
        Member member = memberRepository.save(new Member(
            fixtureName + "@example.com",
            "password",
            fixtureName + " user",
            MemberRole.ROLE_USER
        ));
        MemberAddress address = memberAddressRepository.save(new MemberAddress(
            member,
            "Home",
            "Recipient",
            "01012345678",
            "00000",
            "123 Test-ro",
            "101",
            true
        ));
        Category category = categoryRepository.save(new Category(
            fixtureName + " products",
            fixtureName + "-products"
        ));
        Product product = productRepository.save(new Product(
            category,
            fixtureName + " product",
            "description",
            "YMall",
            BigDecimal.valueOf(10000),
            BigDecimal.ZERO,
            BigDecimal.valueOf(4.5),
            10,
            "thumbnail",
            ProductStatus.APPROVED
        ));
        cartItemRepository.save(new CartItem(member, product, 2));
        return new PaymentOrderData(member, address, product);
    }

    record PaymentOrderData(Member member, MemberAddress address, Product product) {
    }
}
