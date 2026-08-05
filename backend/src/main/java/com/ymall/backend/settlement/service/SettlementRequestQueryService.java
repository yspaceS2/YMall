package com.ymall.backend.settlement.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
import com.ymall.backend.seller.entity.SellerProfile;
import com.ymall.backend.seller.repository.SellerSettlementAccountRepository;
import com.ymall.backend.seller.service.SellerProfileService;
import com.ymall.backend.settlement.dto.SettlementAvailabilityResponse;
import com.ymall.backend.settlement.dto.SettlementRequestHistoryResponse;
import com.ymall.backend.settlement.dto.SettlementRequestResponse;
import com.ymall.backend.settlement.dto.SettlementRequestWorkType;
import com.ymall.backend.settlement.entity.SettlementLedgerEntry;
import com.ymall.backend.settlement.entity.SettlementRequest;
import com.ymall.backend.settlement.entity.SettlementRequestStatus;
import com.ymall.backend.settlement.repository.SettlementLedgerRepository;
import com.ymall.backend.settlement.repository.SettlementRequestHistoryRepository;
import com.ymall.backend.settlement.repository.SettlementRequestRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class SettlementRequestQueryService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Seoul");

    private final SettlementRequestRepository requestRepository;
    private final SettlementRequestHistoryRepository historyRepository;
    private final SettlementLedgerRepository ledgerRepository;
    private final SellerSettlementAccountRepository settlementAccountRepository;
    private final SellerProfileService sellerProfileService;
    private final SettlementAmountCalculator amountCalculator;
    private final SettlementRequestResponseMapper responseMapper;

    SettlementAvailabilityResponse getAvailability(Long memberId) {
        SellerProfile seller = sellerProfileService.getProfileEntity(memberId);
        List<SettlementLedgerEntry> entries = ledgerRepository.findAvailable(seller.getId());
        SettlementAmounts amounts = amountCalculator.calculate(entries);
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

    PageResponse<SettlementRequestResponse> getSellerRequests(
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
        return findRequests(
            seller.getId(),
            status,
            workType,
            requestId,
            null,
            requestedFrom,
            requestedTo,
            page,
            size
        );
    }

    SettlementRequestResponse getSellerRequest(Long memberId, Long requestId) {
        SellerProfile seller = sellerProfileService.getProfileEntity(memberId);
        return requestRepository.findByIdAndSellerProfileId(requestId, seller.getId())
            .map(responseMapper::toResponse)
            .orElseThrow(() -> new BusinessException(
                ErrorCode.SETTLEMENT_REQUEST_NOT_FOUND
            ));
    }

    PageResponse<SettlementRequestResponse> getAdminRequests(
        SettlementRequestStatus status,
        SettlementRequestWorkType workType,
        Long requestId,
        String sellerKeyword,
        LocalDate requestedFrom,
        LocalDate requestedTo,
        int page,
        int size
    ) {
        return findRequests(
            null,
            status,
            workType,
            requestId,
            normalize(sellerKeyword),
            requestedFrom,
            requestedTo,
            page,
            size
        );
    }

    SettlementRequestResponse getAdminRequest(Long requestId) {
        return requestRepository.findById(requestId)
            .map(responseMapper::toResponse)
            .orElseThrow(() -> new BusinessException(
                ErrorCode.SETTLEMENT_REQUEST_NOT_FOUND
            ));
    }

    List<SettlementRequestHistoryResponse> getHistory(Long requestId) {
        if (!requestRepository.existsById(requestId)) {
            throw new BusinessException(ErrorCode.SETTLEMENT_REQUEST_NOT_FOUND);
        }
        return historyRepository.findAllBySettlementRequestIdOrderByCreatedAtAsc(requestId)
            .stream()
            .map(responseMapper::toHistoryResponse)
            .toList();
    }

    private PageResponse<SettlementRequestResponse> findRequests(
        Long sellerProfileId,
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
                    sellerProfileId,
                    filterStatuses(status, workType),
                    requestId,
                    sellerKeyword,
                    startOfDay(requestedFrom),
                    startOfNextDay(requestedTo)
                ),
                pageRequest(page, size)
            )
            .map(responseMapper::toResponse));
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
}
