package com.ymall.backend.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.ymall.backend.member.entity.MemberAddress;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryAddressSnapshot {
    @Column(name = "recipient_name", length = 50)
    private String recipientName;

    @Column(name = "recipient_phone", length = 20)
    private String recipientPhone;

    @Column(name = "postal_code", length = 10)
    private String postalCode;

    @Column(name = "road_address", length = 255)
    private String roadAddress;

    @Column(name = "detail_address", length = 255)
    private String detailAddress;

    public DeliveryAddressSnapshot(MemberAddress address) {
        this.recipientName = address.getRecipientName();
        this.recipientPhone = address.getRecipientPhone();
        this.postalCode = address.getPostalCode();
        this.roadAddress = address.getRoadAddress();
        this.detailAddress = address.getDetailAddress();
    }
}
