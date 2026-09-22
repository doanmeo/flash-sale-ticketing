package com.DTPM.FlashSale.mapper;

import com.DTPM.FlashSale.dto.response.OrderResponse;
import com.DTPM.FlashSale.entity.Order;
import com.DTPM.FlashSale.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {PaymentMapper.class})
public interface OrderMapper {

    @Mapping(target = "payment", source = "payment")
    OrderResponse toOrderResponse(Order order);

    @Mapping(target = "ticketTypeId", source = "ticketType.id")
    @Mapping(target = "ticketTypeName", source = "ticketType.name")
    @Mapping(target = "subtotal", expression = "java(item.calculateSubtotal())")
    OrderResponse.OrderItemResponse toOrderItemResponse(OrderItem item);
}
