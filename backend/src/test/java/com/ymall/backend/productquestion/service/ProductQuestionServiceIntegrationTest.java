package com.ymall.backend.productquestion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.global.security.MemberPrincipal;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.member.repository.MemberRepository;
import com.ymall.backend.notification.repository.NotificationRepository;
import com.ymall.backend.product.entity.Category;
import com.ymall.backend.product.entity.Product;
import com.ymall.backend.product.entity.ProductStatus;
import com.ymall.backend.product.repository.CategoryRepository;
import com.ymall.backend.product.repository.ProductRepository;
import com.ymall.backend.productquestion.dto.ProductQuestionAnswerRequest;
import com.ymall.backend.productquestion.dto.ProductQuestionCreateRequest;
import com.ymall.backend.productquestion.dto.ProductQuestionUpdateRequest;
import com.ymall.backend.productquestion.entity.ProductQuestionStatus;
import com.ymall.backend.seller.entity.SellerProfile;
import com.ymall.backend.seller.repository.SellerProfileRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProductQuestionServiceIntegrationTest {

    @Autowired
    private ProductQuestionService productQuestionService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private SellerProfileRepository sellerProfileRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    void privateQuestionIsHiddenFromGuestsAndVisibleToOwnerAndSeller() {
        Member seller = memberRepository.save(new Member(
            "question-seller@ymall.local",
            "password",
            "판매자",
            MemberRole.ROLE_SELLER
        ));
        Member buyer = memberRepository.save(new Member(
            "question-buyer@ymall.local",
            "password",
            "구매자",
            MemberRole.ROLE_USER
        ));
        Member otherBuyer = memberRepository.save(new Member(
            "question-other-buyer@ymall.local",
            "password",
            "다른 구매자",
            MemberRole.ROLE_USER
        ));
        Member otherSeller = memberRepository.save(new Member(
            "question-other-seller@ymall.local",
            "password",
            "다른 판매자",
            MemberRole.ROLE_SELLER
        ));
        SellerProfile sellerProfile = sellerProfileRepository.save(new SellerProfile(
            seller,
            "문의 테스트 상점",
            "111-22-33333",
            "상품 문의 테스트"
        ));
        sellerProfileRepository.save(new SellerProfile(
            otherSeller,
            "다른 문의 테스트 상점",
            "222-33-44444",
            "권한 검증용 판매자"
        ));
        Category category = categoryRepository.save(new Category(
            "문의 테스트",
            "question-test"
        ));
        Product product = new Product(
            category,
            "문의 테스트 상품",
            "상품 설명",
            "YMALL",
            BigDecimal.valueOf(10_000),
            BigDecimal.ZERO,
            null,
            10,
            "/images/question-product.jpg",
            ProductStatus.APPROVED
        );
        product.assignSellerProfile(sellerProfile);
        productRepository.save(product);

        var created = productQuestionService.create(
            buyer.getId(),
            product.getId(),
            new ProductQuestionCreateRequest(
                "재입고 예정이 있나요?",
                "다음 달 전에 재입고되는지 궁금합니다.",
                true
            )
        );

        var guestQuestion = productQuestionService.getProductQuestions(
            product.getId(),
            null,
            1,
            10
        ).content().get(0);
        assertThat(guestQuestion.contentVisible()).isFalse();
        assertThat(guestQuestion.title()).isEqualTo("비밀 문의입니다");
        assertThat(guestQuestion.content()).isNull();

        var ownerQuestion = productQuestionService.getProductQuestions(
            product.getId(),
            new MemberPrincipal(buyer.getId(), buyer.getEmail(), buyer.getRole()),
            1,
            10
        ).content().get(0);
        assertThat(ownerQuestion.contentVisible()).isTrue();
        assertThat(ownerQuestion.ownedByRequester()).isTrue();

        assertThatThrownBy(() -> productQuestionService.update(
            otherBuyer.getId(),
            created.questionId(),
            new ProductQuestionUpdateRequest("수정 시도", "다른 회원의 문의", false)
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_QUESTION_NOT_FOUND)
        );
        assertThatThrownBy(() -> productQuestionService.answer(
            otherSeller.getId(),
            created.questionId(),
            new ProductQuestionAnswerRequest("다른 판매자의 답변")
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_QUESTION_NOT_FOUND)
        );

        assertThat(productQuestionService.getPendingCount(seller.getId()).count())
            .isEqualTo(1);
        var answered = productQuestionService.answer(
            seller.getId(),
            created.questionId(),
            new ProductQuestionAnswerRequest("다음 주에 재입고될 예정입니다.")
        );
        assertThat(answered.status()).isEqualTo(ProductQuestionStatus.ANSWERED);
        assertThat(answered.answer().content()).contains("다음 주");
        assertThat(productQuestionService.getPendingCount(seller.getId()).count())
            .isZero();
        assertThat(notificationRepository.countByMemberIdAndReadAtIsNull(seller.getId()))
            .isEqualTo(1);
        assertThat(notificationRepository.countByMemberIdAndReadAtIsNull(buyer.getId()))
            .isEqualTo(1);
    }
}
