package com.ymall.backend.settlement.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ymall.backend.global.common.PageResponse;
import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.repository.MemberRepository;
import com.ymall.backend.seller.entity.SellerProfile;
import com.ymall.backend.seller.repository.SellerProfileRepository;
import com.ymall.backend.seller.repository.SellerSettlementAccountRepository;
import com.ymall.backend.seller.service.SellerProfileService;
import com.ymall.backend.settlement.dto.SettlementAvailabilityResponse;
import com.ymall.backend.settlement.dto.SettlementRequestHistoryResponse;
import com.ymall.backend.settlement.dto.SettlementRequestResponse;
import com.ymall.backend.settlement.dto.SettlementRequestWorkType;
import com.ymall.backend.settlement.entity.SettlementLedgerEntry;
import com.ymall.backend.settlement.entity.SettlementRequest;
import com.ymall.backend.settlement.entity.SettlementRequestHistory;
import com.ymall.backend.settlement.entity.SettlementRequestStatus;
import com.ymall.backend.settlement.repository.SettlementLedgerRepository;
import com.ymall.backend.settlement.repository.SettlementRequestHistoryRepository;
import com.ymall.backend.settlement.repository.SettlementRequestRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettlementRequestService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Seoul");

    private final SettlementRequestRepository requestRepository;
    private final SettlementRequestHistoryRepository historyRepository;
    private final SettlementLedgerRepository ledgerRepository;
    private final SellerProfileRepository sellerProfileRepository;
    private final SellerSettlementAccountRepository settlementAccountRepository;
    private final SellerProfileService sellerProfileService;
    private final MemberRepository memberRepository;

    public SettlementAvailabilityResponse getAvailability(Long memberId) {
        SellerProfile seller = sellerProfileService.getProfileEntity(memberId);
        List<SettlementLedgerEntry> entries = ledgerRepository.findAvailable(seller.getId());
        Amounts amounts = amounts(entries);
        boolean hasSettlementAccount = settlementAccountRepository
            .findBySellerProfileId(seller.getId())
            .isPresent();
        return new SettlementAvailabilityResponse(
            entries.size(),
            amounts.gross(),
            amounts.fee(),
            amounts.settlement(),
            hasSettlementAccount,
            hasSettlementAccount && amounts.settlement().compareTo(BigDecimal.ZERO) > 0
        );
    }

    public PageResponse<SettlementRequestResponse> getSellerRequests(
        Long memberId,
        SettlementRequestStatus status,
        SettlementRequestWorkType workType,
        Long requestId,
        LocalDate requestedFrom,
        LocalDate requestedTo,
        int page,
        int size
    ) {
        SellerProfile seller = sellerProfileService.getProfileEntity(memberId);
        return PageResponse.from(requestRepository
            .findAll(
                requestSpecification(
                    seller.getId(),
                    filterStatuses(status, workType),
                    requestId,
                    null,
                    startOfDay(requestedFrom),
                    startOfNextDay(requestedTo)
                ),
                pageRequest(page, size)
            )
            .map(this::toResponse));
    }

    public SettlementRequestResponse getSellerRequest(Long memberId, Long requestId) {
        SellerProfile seller = sellerProfileService.getProfileEntity(memberId);
        return requestRepository.findByIdAndSellerProfileId(requestId, seller.getId())
            .map(this::toResponse)
            .orElseThrow(() -> new BusinessException(
                ErrorCode.SETTLEMENT_REQUEST_NOT_FOUND
            ));
    }

    @Transactional
    public SettlementRequestResponse request(Long memberId) {
        SellerProfile seller = sellerProfileRepository.findForUpdateByMemberId(memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_PROFILE_NOT_FOUND));
        if (settlementAccountRepository.findBySellerProfileId(seller.getId()).isEmpty()) {
            throw new BusinessException(ErrorCode.SELLER_SETTLEMENT_ACCOUNT_NOT_FOUND);
        }
        List<SettlementLedgerEntry> entries = ledgerRepository.findAvailableForUpdate(
            seller.getId()
        );
        Amounts amounts = amounts(entries);
        if (entries.isEmpty() || amounts.settlement().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.SETTLEMENT_REQUEST_AMOUNT_INVALID);
        }

        Member actor = seller.getMember();
        SettlementRequest request = requestRepository.save(new SettlementRequest(
            seller,
            amounts.gross(),
            amounts.fee(),
            amounts.settlement()
        ));

        entries.forEach(entry -> entry.requestSettlement(request));
        saveHistory(request, null, SettlementRequestStatus.REQUESTED, actor, null);
        return toResponse(request);
    }

    public PageResponse<SettlementRequestResponse> getAdminRequests(
        SettlementRequestStatus status,
        SettlementRequestWorkType workType,
        Long requestId,
        String sellerKeyword,
        LocalDate requestedFrom,
        LocalDate requestedTo,
        int page,
        int size
    ) {
        return PageResponse.from(requestRepository
            .findAll(
                requestSpecification(
                    null,
                    filterStatuses(status, workType),
                    requestId,
                    normalize(sellerKeyword),
                    startOfDay(requestedFrom),
                    startOfNextDay(requestedTo)
                ),
                pageRequest(page, size)
            )
            .map(this::toResponse));
    }

    public SettlementRequestResponse getAdminRequest(Long requestId) {
        return requestRepository.findById(requestId)
            .map(this::toResponse)
            .orElseThrow(() -> new BusinessException(
                ErrorCode.SETTLEMENT_REQUEST_NOT_FOUND
            ));
    }

    @Transactional
    public SettlementRequestResponse approve(Long adminMemberId, Long requestId) {
        SettlementRequest request = requestForUpdate(requestId);
        Member admin = member(adminMemberId);
        try {
            SettlementRequestStatus previous = request.approve(admin);
            saveHistory(request, previous, SettlementRequestStatus.APPROVED, admin, null);
            return toResponse(request);
        } catch (IllegalStateException exception) {
            throw new BusinessException(ErrorCode.SETTLEMENT_REQUEST_STATUS_INVALID);
        }
    }

    @Transactional
    public SettlementRequestResponse reject(
        Long adminMemberId,
        Long requestId,
        String reason
    ) {
        SettlementRequest request = requestForUpdate(requestId);
        Member admin = member(adminMemberId);
        try {
            SettlementRequestStatus previous = request.reject(admin, reason.trim());
            ledgerRepository.findAllBySettlementRequestId(requestId)
                .forEach(SettlementLedgerEntry::releaseSettlementRequest);
            saveHistory(
                request,
                previous,
                SettlementRequestStatus.REJECTED,
                admin,
                reason.trim()
            );
            return toResponse(request);
        } catch (IllegalStateException exception) {
            throw new BusinessException(ErrorCode.SETTLEMENT_REQUEST_STATUS_INVALID);
        }
    }

    @Transactional
    public SettlementRequestResponse completeMockPayment(
        Long adminMemberId,
        Long requestId
    ) {
        SettlementRequest request = requestForUpdate(requestId);
        Member admin = member(adminMemberId);
        String reference = "MOCK-" + UUID.randomUUID();
        try {
            SettlementRequestStatus previous = request.markPaid(admin, reference);
            ledgerRepository.findAllBySettlementRequestId(requestId)
                .forEach(SettlementLedgerEntry::markPaid);
            saveHistory(request, previous, SettlementRequestStatus.PAID, admin, reference);
            return toResponse(request);
        } catch (IllegalStateException exception) {
            throw new BusinessException(ErrorCode.SETTLEMENT_REQUEST_STATUS_INVALID);
        }
    }

    public List<SettlementRequestHistoryResponse> getHistory(Long requestId) {
        if (!requestRepository.existsById(requestId)) {
            throw new BusinessException(ErrorCode.SETTLEMENT_REQUEST_NOT_FOUND);
        }
        return historyRepository.findAllBySettlementRequestIdOrderByCreatedAtAsc(requestId)
            .stream()
            .map(history -> new SettlementRequestHistoryResponse(
                history.getFromStatus(),
                history.getToStatus(),
                history.getActor().getId(),
                history.getActor().getName(),
                history.getReason(),
                history.getCreatedAt()
            ))
            .toList();
    }

    private Amounts amounts(List<SettlementLedgerEntry> entries) {
        return entries.stream().reduce(
            new Amounts(ZERO, ZERO, ZERO),
            (sum, entry) -> new Amounts(
                sum.gross().add(entry.getGrossAmount()),
                sum.fee().add(entry.getFeeAmount()),
                sum.settlement().add(entry.getSettlementAmount())
            ),
            (left, right) -> new Amounts(
                left.gross().add(right.gross()),
                left.fee().add(right.fee()),
                left.settlement().add(right.settlement())
            )
        );
    }

    private void saveHistory(
        SettlementRequest request,
        SettlementRequestStatus from,
        SettlementRequestStatus to,
        Member actor,
        String reason
    ) {
        historyRepository.save(new SettlementRequestHistory(request, from, to, actor, reason));
    }

    private SettlementRequest requestForUpdate(Long requestId) {
        return requestRepository.findByIdForUpdate(requestId)
            .orElseThrow(() -> new BusinessException(ErrorCode.SETTLEMENT_REQUEST_NOT_FOUND));
    }

    private Member member(Long memberId) {
        return memberRepository.findById(memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    private PageRequest pageRequest(int page, int size) {
        return PageRequest.of(
            Math.max(page - 1, 0),
            Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
            Sort.by(Sort.Direction.DESC, "createdAt", "id")
        );
    }

    private Specification<SettlementRequest> requestSpecification(
        Long sellerProfileId,
        List<SettlementRequestStatus> statuses,
        Long requestId,
        String sellerKeyword,
        Instant requestedFrom,
        Instant requestedToExclusive
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (sellerProfileId != null) {
                predicates.add(criteriaBuilder.equal(
                    root.get("sellerProfile").get("id"),
                    sellerProfileId
                ));
            }
            if (!statuses.isEmpty()) {
                predicates.add(root.get("status").in(statuses));
            }
            if (requestId != null) {
                predicates.add(criteriaBuilder.equal(root.get("id"), requestId));
            }
            if (sellerKeyword != null) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("sellerProfile").get("storeName")),
                    "%" + sellerKeyword.toLowerCase(Locale.ROOT) + "%"
                ));
            }
            if (requestedFrom != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                    root.get("createdAt"),
                    requestedFrom
                ));
            }
            if (requestedToExclusive != null) {
                predicates.add(criteriaBuilder.lessThan(
                    root.get("createdAt"),
                    requestedToExclusive
                ));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private List<SettlementRequestStatus> filterStatuses(
        SettlementRequestStatus status,
        SettlementRequestWorkType workType
    ) {
        if (workType != null) {
            return List.of(
                SettlementRequestStatus.REQUESTED,
                SettlementRequestStatus.APPROVED
            );
        }
        return status == null ? List.of() : List.of(status);
    }

    private Instant startOfDay(LocalDate date) {
        return date == null ? null : date.atStartOfDay(BUSINESS_ZONE).toInstant();
    }

    private Instant startOfNextDay(LocalDate date) {
        return date == null
            ? null
            : date.plusDays(1).atStartOfDay(BUSINESS_ZONE).toInstant();
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private SettlementRequestResponse toResponse(SettlementRequest request) {
        return new SettlementRequestResponse(
            request.getId(),
            request.getSellerProfile().getId(),
            request.getSellerProfile().getStoreName(),
            request.getPeriodStart(),
            request.getPeriodEnd(),
            request.getStatus(),
            request.getGrossAmount(),
            request.getFeeAmount(),
            request.getSettlementAmount(),
            request.getRejectionReason(),
            request.getMockPaymentReference(),
            request.getReviewedAt(),
            request.getPaidAt(),
            request.getCreatedAt(),
            request.getUpdatedAt()
        );
    }

    private record Amounts(
        BigDecimal gross,
        BigDecimal fee,
        BigDecimal settlement
    ) {
    }
}
