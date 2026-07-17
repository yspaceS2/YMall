package com.ymall.backend.seller.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.ymall.backend.seller.dto.SellerProfileResponse;
import com.ymall.backend.seller.entity.SellerProfile;

@Mapper(componentModel = "spring")
public interface SellerProfileMapper {

    @Mapping(source = "id", target = "sellerProfileId")
    @Mapping(source = "member.id", target = "memberId")
    SellerProfileResponse toResponse(SellerProfile sellerProfile);
}
