package com.ymall.backend.settlement.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
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

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Seoul");
    private static final int MAX_PAGE_SIZE = 100;
    private static final BigDecimal ZERO = new BigDecimal("0.00");

    private final SettlementRequestRepository requestRepository;
    private final SettlementRequestHistoryRepository historyRepository;
    private final SettlementLedgerRepository ledgerRepository;
    private final SellerProfileRepository sellerProfileRepository;
    private final SellerSettlementAccountRepository settlementAccountRepository;
    private final SellerProfileService sellerProfileService;
    private final MemberRepository memberRepository;

    public SettlementAvailabilityResponse getAvailability(Long memberId, String periodText) {
        SellerProfile seller = sellerProfileService.getProfileEntity(memberId);
        Period period = period(periodText);
        List<SettlementLedgerEntry> entries = ledgerRepository.findAvailableForPeriod(
            seller.getId(),
            period.from(),
            period.to()
        );
        Amounts amounts = amounts(entries);
        boolean requestOpen = requestRepository
            .findBySellerProfileIdAndPeriodStart(seller.getId(), period.start())
            .map(request -> request.getStatus() == SettlementRequestStatus.REJECTED)
            .orElse(true);
        boolean hasSettlementAccount = settlementAccountRepository
            .findBySellerProfileId(seller.getId())
            .isPresent();
        return new SettlementAvailabilityResponse(
            period.start(),
            period.end(),
            entries.size(),
            amounts.gross(),
            amounts.fee(),
            amounts.settlement(),
            hasSettlementAccount,
            hasSettlementAccount
                && requestOpen
                && amounts.settlement().compareTo(BigDecimal.ZERO) > 0
        );
    }

    public PageResponse<SettlementRequestResponse> getSellerRequests(
        Long memberId,
        int page,
        int size
    ) {
        SellerProfile seller = sellerProfileService.getProfileEntity(memberId);
        return PageResponse.from(requestRepository
            .findAllBySellerProfileIdOrderByPeriodStartDesc(
                seller.getId(),
                pageRequest(page, size)
            )
            .map(this::toResponse));
    }

    @Transactional
    public SettlementRequestResponse request(Long memberId, String periodText) {
        SellerProfile seller = sellerProfileRepository.findForUpdateByMemberId(memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_PROFILE_NOT_FOUND));
        if (settlementAccountRepository.findBySellerProfileId(seller.getId()).isEmpty()) {
            throw new BusinessException(ErrorCode.SELLER_SETTLEMENT_ACCOUNT_NOT_FOUND);
        }
        Period period = period(periodText);
        SettlementRequest existing = requestRepository
            .findBySellerAndPeriodForUpdate(seller.getId(), period.start())
            .orElse(null);
        if (existing != null && existing.getStatus() != SettlementRequestStatus.REJECTED) {
            throw new BusinessException(ErrorCode.SETTLEMENT_REQUEST_DUPLICATED);
        }
        List<SettlementLedgerEntry> entries = ledgerRepository.findAvailableForPeriodForUpdate(
            seller.getId(),
            period.from(),
            period.to()
        );
        Amounts amounts = amounts(entries);
        if (entries.isEmpty() || amounts.settlement().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.SETTLEMENT_REQUEST_AMOUNT_INVALID);
        }

        Member actor = seller.getMember();
        SettlementRequest request;
        SettlementRequestStatus fromStatus = null;
        if (existing == null) {
            request = requestRepository.save(new SettlementRequest(
                seller,
                period.start(),
                period.end(),
                amounts.gross(),
                amounts.fee(),
                amounts.settlement()
            ));
        } else {
            fromStatus = existing.resubmit(
                amounts.gross(),
                amounts.fee(),
                amounts.settlement()
            );
            request = existing;
        }

        entries.forEach(entry -> entry.requestSettlement(request));
        saveHistory(request, fromStatus, SettlementRequestStatus.REQUESTED, actor, null);
        return toResponse(request);
    }

    public PageResponse<SettlementRequestResponse> getAdminRequests(
        SettlementRequestStatus status,
        int page,
        int size
    ) {
        return PageResponse.from(requestRepository
            .findAdminRequests(status, pageRequest(page, size))
            .map(this::toResponse));
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

    private Period period(String text) {
        YearMonth target;
        try {
            target = YearMonth.parse(text);
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.SETTLEMENT_REQUEST_PERIOD_INVALID);
        }
        if (!target.isBefore(YearMonth.now(BUSINESS_ZONE))) {
            throw new BusinessException(ErrorCode.SETTLEMENT_REQUEST_PERIOD_INVALID);
        }
        LocalDate start = target.atDay(1);
        LocalDate nextStart = target.plusMonths(1).atDay(1);
        return new Period(
            start,
            target.atEndOfMonth(),
            start.atStartOfDay(BUSINESS_ZONE).toInstant(),
            nextStart.atStartOfDay(BUSINESS_ZONE).toInstant()
        );
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
            Math.min(Math.max(size, 1), MAX_PAGE_SIZE)
        );
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

    private record Period(
        LocalDate start,
        LocalDate end,
        Instant from,
        Instant to
    ) {
    }

    private record Amounts(
        BigDecimal gross,
        BigDecimal fee,
        BigDecimal settlement
    ) {
    }
}
