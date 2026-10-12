package com.DTPM.FlashSale.mapper;

import com.DTPM.FlashSale.dto.response.PaymentResponse;
import com.DTPM.FlashSale.domain.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    @Mapping(target = "orderId", source = "order.id")
    @Mapping(target = "orderCode", source = "order.orderCode")
    @Mapping(target = "paymentStatus", source = "status")
    @Mapping(target = "orderStatus", source = "order.status")
    @Mapping(target = "qrCodeUrl", ignore = true)
    PaymentResponse toPaymentResponse(Payment payment);
}
