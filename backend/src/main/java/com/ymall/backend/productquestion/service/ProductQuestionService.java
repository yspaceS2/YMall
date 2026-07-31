package com.ymall.backend.productquestion.service;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ymall.backend.global.common.PageResponse;
import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.global.security.MemberPrincipal;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.member.repository.MemberRepository;
import com.ymall.backend.notification.entity.NotificationType;
import com.ymall.backend.notification.event.NotificationEvent;
import com.ymall.backend.notification.service.NotificationService;
import com.ymall.backend.product.entity.Product;
import com.ymall.backend.product.entity.ProductStatus;
import com.ymall.backend.product.repository.ProductRepository;
import com.ymall.backend.productquestion.dto.ProductQuestionAnswerRequest;
import com.ymall.backend.productquestion.dto.ProductQuestionAnswerResponse;
import com.ymall.backend.productquestion.dto.ProductQuestionCreateRequest;
import com.ymall.backend.productquestion.dto.ProductQuestionPendingCountResponse;
import com.ymall.backend.productquestion.dto.ProductQuestionResponse;
import com.ymall.backend.productquestion.dto.ProductQuestionUpdateRequest;
import com.ymall.backend.productquestion.entity.ProductQuestion;
import com.ymall.backend.productquestion.entity.ProductQuestionAnswer;
import com.ymall.backend.productquestion.entity.ProductQuestionStatus;
import com.ymall.backend.productquestion.repository.ProductQuestionAnswerRepository;
import com.ymall.backend.productquestion.repository.ProductQuestionRepository;
import com.ymall.backend.seller.entity.SellerProfile;
import com.ymall.backend.seller.service.SellerProfileService;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductQuestionService {

    private static final int MAX_PAGE_SIZE = 100;

    private final ProductQuestionRepository questionRepository;
    private final ProductQuestionAnswerRepository answerRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;
    private final SellerProfileService sellerProfileService;
    private final NotificationService notificationService;

    public PageResponse<ProductQuestionResponse> getProductQuestions(
        Long productId,
        MemberPrincipal viewer,
        int page,
        int size
    ) {
        if (!productRepository.existsByIdAndStatus(productId, ProductStatus.APPROVED)) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        return PageResponse.from(
            questionRepository.findByProductIdOrderByCreatedAtDesc(
                productId,
                pageRequest(page, size)
            ).map(question -> toResponse(question, viewer, false))
        );
    }

    @Transactional
    public ProductQuestionResponse create(
        Long memberId,
        Long productId,
        ProductQuestionCreateRequest request
    ) {
        Product product = productRepository.findById(productId)
            .filter(candidate -> candidate.getStatus() == ProductStatus.APPROVED)
            .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        if (product.getSellerProfile() == null) {
            throw new BusinessException(ErrorCode.PRODUCT_QUESTION_NOT_ALLOWED);
        }
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        ProductQuestion question = questionRepository.save(new ProductQuestion(
            product,
            member,
            request.title().trim(),
            request.content().trim(),
            request.privateQuestion()
        ));
        notifySeller(question);
        return toResponse(
            question,
            new MemberPrincipal(member.getId(), member.getEmail(), member.getRole()),
            false
        );
    }

    @Transactional
    public ProductQuestionResponse update(
        Long memberId,
        Long questionId,
        ProductQuestionUpdateRequest request
    ) {
        ProductQuestion question = getOwnedQuestion(questionId, memberId);
        if (question.getStatus() == ProductQuestionStatus.ANSWERED) {
            throw new BusinessException(ErrorCode.PRODUCT_QUESTION_ALREADY_ANSWERED);
        }
        question.update(
            request.title().trim(),
            request.content().trim(),
            request.privateQuestion()
        );
        return toResponse(
            question,
            new MemberPrincipal(
                question.getMember().getId(),
                question.getMember().getEmail(),
                question.getMember().getRole()
            ),
            false
        );
    }

    @Transactional
    public void delete(Long memberId, Long questionId) {
        questionRepository.delete(getOwnedQuestion(questionId, memberId));
    }

    public PageResponse<ProductQuestionResponse> getSellerQuestions(
        Long memberId,
        int page,
        int size,
        ProductQuestionStatus status,
        String keyword
    ) {
        SellerProfile profile = sellerProfileService.getProfileEntity(memberId);
        Page<ProductQuestion> questions = questionRepository.searchSellerQuestions(
            profile.getId(),
            status != null,
            status == null ? ProductQuestionStatus.WAITING : status,
            keyword == null ? "" : keyword.trim(),
            pageRequest(page, size)
        );
        return PageResponse.from(questions.map(question -> toResponse(question, null, true)));
    }

    public ProductQuestionResponse getSellerQuestion(Long memberId, Long questionId) {
        SellerProfile profile = sellerProfileService.getProfileEntity(memberId);
        ProductQuestion question = questionRepository
            .findByIdAndProductSellerProfileId(questionId, profile.getId())
            .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_QUESTION_NOT_FOUND));
        return toResponse(question, null, true);
    }

    public ProductQuestionPendingCountResponse getPendingCount(Long memberId) {
        SellerProfile profile = sellerProfileService.getProfileEntity(memberId);
        return new ProductQuestionPendingCountResponse(
            questionRepository.countByProductSellerProfileIdAndStatus(
                profile.getId(),
                ProductQuestionStatus.WAITING
            )
        );
    }

    @Transactional
    public ProductQuestionResponse answer(
        Long memberId,
        Long questionId,
        ProductQuestionAnswerRequest request
    ) {
        SellerProfile profile = sellerProfileService.getProfileEntity(memberId);
        ProductQuestion question = questionRepository
            .findByIdAndProductSellerProfileId(questionId, profile.getId())
            .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_QUESTION_NOT_FOUND));
        String content = request.content().trim();
        boolean firstAnswer = question.getAnswer() == null;
        if (firstAnswer) {
            ProductQuestionAnswer answer = answerRepository.save(
                new ProductQuestionAnswer(question, profile, content)
            );
            question.attachAnswer(answer);
        } else {
            question.getAnswer().update(content);
        }
        if (firstAnswer) {
            notifyRequester(question);
        }
        return toResponse(question, null, true);
    }

    private ProductQuestion getOwnedQuestion(Long questionId, Long memberId) {
        return questionRepository.findByIdAndMemberId(questionId, memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_QUESTION_NOT_FOUND));
    }

    private ProductQuestionResponse toResponse(
        ProductQuestion question,
        MemberPrincipal viewer,
        boolean sellerView
    ) {
        boolean ownedByRequester = viewer != null
            && question.getMember().getId().equals(viewer.memberId());
        boolean adminView = viewer != null && viewer.role() == MemberRole.ROLE_ADMIN;
        boolean productSellerView = viewer != null
            && question.getProduct().getSellerProfile() != null
            && question.getProduct().getSellerProfile().getMember().getId()
                .equals(viewer.memberId());
        boolean contentVisible = !question.isPrivateQuestion()
            || ownedByRequester
            || adminView
            || productSellerView
            || sellerView;
        return new ProductQuestionResponse(
            question.getId(),
            question.getProduct().getId(),
            question.getProduct().getName(),
            question.getProduct().getThumbnailUrl(),
            contentVisible ? maskName(question.getMember().getName()) : "비공개",
            contentVisible ? question.getTitle() : "비밀 문의입니다",
            contentVisible ? question.getContent() : null,
            question.isPrivateQuestion(),
            ownedByRequester,
            contentVisible,
            question.getStatus(),
            contentVisible ? toAnswerResponse(question.getAnswer()) : null,
            question.getCreatedAt(),
            question.getUpdatedAt()
        );
    }

    private ProductQuestionAnswerResponse toAnswerResponse(ProductQuestionAnswer answer) {
        if (answer == null) {
            return null;
        }
        return new ProductQuestionAnswerResponse(
            answer.getId(),
            answer.getContent(),
            answer.getCreatedAt(),
            answer.getUpdatedAt()
        );
    }

    private void notifySeller(ProductQuestion question) {
        Long sellerMemberId = question.getProduct().getSellerProfile().getMember().getId();
        if (sellerMemberId.equals(question.getMember().getId())) {
            return;
        }
        notificationService.create(new NotificationEvent(
            UUID.randomUUID(),
            sellerMemberId,
            NotificationType.PRODUCT_QUESTION_CREATED,
            "새 상품 문의가 등록되었습니다",
            "%s 상품에 새로운 문의가 등록되었습니다."
                .formatted(question.getProduct().getName()),
            "/seller/questions/%d".formatted(question.getId())
        ));
    }

    private void notifyRequester(ProductQuestion question) {
        notificationService.create(new NotificationEvent(
            UUID.randomUUID(),
            question.getMember().getId(),
            NotificationType.PRODUCT_QUESTION_ANSWERED,
            "상품 문의 답변이 등록되었습니다",
            "%s 상품 문의에 판매자 답변이 등록되었습니다."
                .formatted(question.getProduct().getName()),
            "/products/%d?tab=qna".formatted(question.getProduct().getId())
        ));
    }

    private String maskName(String name) {
        if (name == null || name.isBlank()) {
            return "구매자";
        }
        return name.substring(0, 1) + "*".repeat(Math.max(name.length() - 1, 1));
    }

    private PageRequest pageRequest(int page, int size) {
        return PageRequest.of(
            Math.max(page - 1, 0),
            Math.min(Math.max(size, 1), MAX_PAGE_SIZE)
        );
    }
}
