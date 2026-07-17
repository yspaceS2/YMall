package com.ymall.backend.payment.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.ymall.backend.payment.dto.PaymentResponse;
import com.ymall.backend.payment.entity.Payment;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    @Mapping(source = "id", target = "paymentId")
    @Mapping(source = "order.id", target = "orderId")
    PaymentResponse toPaymentResponse(Payment payment);
}
