package com.DTPM.FlashSale.mapper;

import com.DTPM.FlashSale.dto.request.CreateTicketTypeRequest;
import com.DTPM.FlashSale.dto.request.UpdateTicketTypeRequest;
import com.DTPM.FlashSale.dto.response.TicketTypeResponse;
import com.DTPM.FlashSale.entity.TicketType;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface TicketTypeMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "availableQuantity", source = "totalQuantity")
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "event", ignore = true)
    TicketType toEntity(CreateTicketTypeRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "availableQuantity", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "event", ignore = true)
    void updateEntityFromRequest(UpdateTicketTypeRequest request, @MappingTarget TicketType ticketType);

    @Mapping(target = "eventId", source = "event.id")
    @Mapping(target = "isOnSale", expression = "java(ticketType.isOnSale())")
    @Mapping(target = "realtimeAvailable", ignore = true)
    TicketTypeResponse toTicketTypeResponse(TicketType ticketType);
}
