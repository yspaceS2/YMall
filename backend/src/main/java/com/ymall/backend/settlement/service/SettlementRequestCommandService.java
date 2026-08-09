package com.ymall.backend.settlement.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ymall.backend.dashboard.service.DashboardRealtimePublisher;
import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.repository.MemberRepository;
import com.ymall.backend.seller.entity.SellerProfile;
import com.ymall.backend.seller.repository.SellerProfileRepository;
import com.ymall.backend.seller.repository.SellerSettlementAccountRepository;
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
class SettlementRequestCommandService {

    private final SettlementRequestRepository requestRepository;
    private final SettlementRequestHistoryRepository historyRepository;
    private final SettlementLedgerRepository ledgerRepository;
    private final SellerProfileRepository sellerProfileRepository;
    private final SellerSettlementAccountRepository settlementAccountRepository;
    private final MemberRepository memberRepository;
    private final DashboardRealtimePublisher dashboardRealtimePublisher;
    private final SettlementAmountCalculator amountCalculator;
    private final SettlementRequestResponseMapper responseMapper;

    @Transactional
    SettlementRequestResponse request(Long memberId) {
        SellerProfile seller = sellerProfileRepository.findForUpdateByMemberId(memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_PROFILE_NOT_FOUND));
        if (settlementAccountRepository.findBySellerProfileId(seller.getId()).isEmpty()) {
            throw new BusinessException(ErrorCode.SELLER_SETTLEMENT_ACCOUNT_NOT_FOUND);
        }
        List<SettlementLedgerEntry> entries = ledgerRepository.findAvailableForUpdate(
            seller.getId()
        );
        SettlementAmounts amounts = amountCalculator.calculate(entries);
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
        invalidateSettlement(request);
        return responseMapper.toResponse(request);
    }

    @Transactional
    SettlementRequestResponse approve(Long adminMemberId, Long requestId) {
        SettlementRequest request = requestForUpdate(requestId);
        Member admin = member(adminMemberId);
        try {
            SettlementRequestStatus previous = request.approve(admin);
            saveHistory(request, previous, SettlementRequestStatus.APPROVED, admin, null);
            invalidateSettlement(request);
            return responseMapper.toResponse(request);
        } catch (IllegalStateException exception) {
            throw new BusinessException(ErrorCode.SETTLEMENT_REQUEST_STATUS_INVALID);
        }
    }

    @Transactional
    SettlementRequestResponse reject(
        Long adminMemberId,
        Long requestId,
        String reason
    ) {
        SettlementRequest request = requestForUpdate(requestId);
        Member admin = member(adminMemberId);
        try {
            String normalizedReason = reason.trim();
            SettlementRequestStatus previous = request.reject(admin, normalizedReason);
            ledgerRepository.findAllBySettlementRequestId(requestId)
                .forEach(SettlementLedgerEntry::releaseSettlementRequest);
            saveHistory(
                request,
                previous,
                SettlementRequestStatus.REJECTED,
                admin,
                normalizedReason
            );
            invalidateSettlement(request);
            return responseMapper.toResponse(request);
        } catch (IllegalStateException exception) {
            throw new BusinessException(ErrorCode.SETTLEMENT_REQUEST_STATUS_INVALID);
        }
    }

    @Transactional
    SettlementRequestResponse completeMockPayment(
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
            invalidateSettlement(request);
            return responseMapper.toResponse(request);
        } catch (IllegalStateException exception) {
            throw new BusinessException(ErrorCode.SETTLEMENT_REQUEST_STATUS_INVALID);
        }
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

    private void invalidateSettlement(SettlementRequest request) {
        dashboardRealtimePublisher.invalidateSellerAndAdmins(
            request.getSellerProfile().getMember().getId(),
            "settlementRequest",
            request.getId()
        );
    }

    private Member member(Long memberId) {
        return memberRepository.findById(memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
