package com.ymall.backend.member.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.member.dto.MemberAddressRequest;
import com.ymall.backend.member.dto.MemberAddressResponse;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.entity.MemberAddress;
import com.ymall.backend.member.repository.MemberAddressRepository;
import com.ymall.backend.member.repository.MemberRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberAddressService {
    private final MemberRepository memberRepository;
    private final MemberAddressRepository memberAddressRepository;

    public List<MemberAddressResponse> getAddresses(Long memberId) {
        return memberAddressRepository.findAllByMemberIdOrderByIsDefaultDescCreatedAtAsc(memberId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public MemberAddressResponse createAddress(Long memberId, MemberAddressRequest request) {
        Member member = findMemberForUpdate(memberId);
        boolean makeDefault = request.isDefault() || !memberAddressRepository.existsByMemberId(memberId);
        if (makeDefault) {
            clearDefaultAddress(memberId);
        }
        MemberAddress address = new MemberAddress(member, request.addressName(), request.recipientName(),
            request.recipientPhone(), request.postalCode(), request.roadAddress(),
            request.detailAddress(), makeDefault);
        return toResponse(memberAddressRepository.save(address));
    }

    @Transactional
    public MemberAddressResponse updateAddress(Long memberId, Long addressId, MemberAddressRequest request) {
        findMemberForUpdate(memberId);
        MemberAddress address = findAddress(memberId, addressId);
        address.update(request.addressName(), request.recipientName(), request.recipientPhone(),
            request.postalCode(), request.roadAddress(), request.detailAddress());
        if (request.isDefault() && !address.isDefault()) {
            clearDefaultAddress(memberId);
            address.makeDefault();
        }
        return toResponse(address);
    }

    @Transactional
    public void deleteAddress(Long memberId, Long addressId) {
        findMemberForUpdate(memberId);
        MemberAddress address = findAddress(memberId, addressId);
        boolean wasDefault = address.isDefault();
        memberAddressRepository.delete(address);
        memberAddressRepository.flush();
        if (wasDefault) {
            memberAddressRepository.findAllByMemberIdOrderByIsDefaultDescCreatedAtAsc(memberId)
                .stream()
                .findFirst()
                .ifPresent(MemberAddress::makeDefault);
        }
    }

    private Member findMemberForUpdate(Long memberId) {
        return memberRepository.findByIdForUpdate(memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    private MemberAddress findAddress(Long memberId, Long addressId) {
        return memberAddressRepository.findByIdAndMemberId(addressId, memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_ADDRESS_NOT_FOUND));
    }

    private void clearDefaultAddress(Long memberId) {
        memberAddressRepository.findFirstByMemberIdAndIsDefaultTrue(memberId)
            .ifPresent(MemberAddress::clearDefault);
    }

    private MemberAddressResponse toResponse(MemberAddress address) {
        return new MemberAddressResponse(address.getId(), address.getAddressName(),
            address.getRecipientName(), address.getRecipientPhone(), address.getPostalCode(),
            address.getRoadAddress(), address.getDetailAddress(), address.isDefault());
    }
}
