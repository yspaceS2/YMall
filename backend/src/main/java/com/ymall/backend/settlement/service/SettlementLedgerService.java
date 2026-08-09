package com.ymall.backend.settlement.service;

import java.time.Instant;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ymall.backend.global.common.PageResponse;
import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.seller.entity.SellerProfile;
import com.ymall.backend.seller.service.SellerProfileService;
import com.ymall.backend.settlement.dto.SettlementLedgerResponse;
import com.ymall.backend.settlement.entity.SettlementLedgerEntry;
import com.ymall.backend.settlement.entity.SettlementStatus;
import com.ymall.backend.settlement.repository.SettlementLedgerRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettlementLedgerService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Instant EARLIEST_LEDGER_INSTANT = Instant.parse(
        "0001-01-01T00:00:00Z"
    );
    private static final Instant LATEST_LEDGER_INSTANT = Instant.parse(
        "9999-12-31T23:59:59Z"
    );

    private final SettlementLedgerRepository ledgerRepository;
    private final SellerProfileService sellerProfileService;

    public PageResponse<SettlementLedgerResponse> getSellerLedger(
        Long memberId,
        SettlementStatus status,
        Instant from,
        Instant to,
        int page,
        int size
    ) {
        if (from != null && to != null && !from.isBefore(to)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        SellerProfile sellerProfile = sellerProfileService.getProfileEntity(memberId);
        Pageable pageable = PageRequest.of(
            Math.max(page - 1, 0),
            Math.min(Math.max(size, 1), MAX_PAGE_SIZE)
        );
        return PageResponse.from(ledgerRepository.findSellerLedger(
            sellerProfile.getId(),
            status,
            from == null ? EARLIEST_LEDGER_INSTANT : from,
            to == null ? LATEST_LEDGER_INSTANT : to,
            pageable
        ).map(this::toResponse));
    }

    private SettlementLedgerResponse toResponse(SettlementLedgerEntry entry) {
        return new SettlementLedgerResponse(
            entry.getId(),
            entry.getOrder().getId(),
            entry.getOrderItem().getId(),
            entry.getEntryType(),
            entry.getStatus(),
            entry.getGrossAmount(),
            entry.getFeeAmount(),
            entry.getSettlementAmount(),
            entry.getOccurredAt()
        );
    }
}
