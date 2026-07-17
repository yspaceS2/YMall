package com.ymall.backend.seller.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.repository.MemberRepository;
import com.ymall.backend.seller.dto.SellerProfileCreateRequest;
import com.ymall.backend.seller.dto.SellerProfileResponse;
import com.ymall.backend.seller.dto.SellerProfileUpdateRequest;
import com.ymall.backend.seller.entity.SellerProfile;
import com.ymall.backend.seller.mapper.SellerProfileMapper;
import com.ymall.backend.seller.repository.SellerProfileRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerProfileService {

    private final SellerProfileRepository sellerProfileRepository;
    private final MemberRepository memberRepository;
    private final SellerProfileMapper sellerProfileMapper;

    public SellerProfileResponse getProfile(Long memberId) {
        return sellerProfileMapper.toResponse(getProfileEntity(memberId));
    }

    @Transactional
    public SellerProfileResponse createProfile(Long memberId, SellerProfileCreateRequest request) {
        if (sellerProfileRepository.existsByMemberId(memberId)) {
            throw new BusinessException(ErrorCode.SELLER_PROFILE_ALREADY_EXISTS);
        }
        if (sellerProfileRepository.existsByBusinessNumber(request.businessNumber())) {
            throw new BusinessException(ErrorCode.SELLER_BUSINESS_NUMBER_DUPLICATED);
        }
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        try {
            SellerProfile savedProfile = sellerProfileRepository.saveAndFlush(new SellerProfile(
                member,
                request.storeName(),
                request.businessNumber(),
                request.description()
            ));
            return sellerProfileMapper.toResponse(savedProfile);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.SELLER_BUSINESS_NUMBER_DUPLICATED);
        }
    }

    @Transactional
    public SellerProfileResponse updateProfile(Long memberId, SellerProfileUpdateRequest request) {
        SellerProfile profile = getProfileEntity(memberId);
        profile.update(request.storeName(), request.description());
        return sellerProfileMapper.toResponse(profile);
    }

    public SellerProfile getProfileEntity(Long memberId) {
        return sellerProfileRepository.findByMemberId(memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_PROFILE_NOT_FOUND));
    }
}
