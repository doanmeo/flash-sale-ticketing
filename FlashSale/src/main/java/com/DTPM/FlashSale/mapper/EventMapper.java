package com.DTPM.FlashSale.mapper;

import com.DTPM.FlashSale.dto.request.CreateEventRequest;
import com.DTPM.FlashSale.dto.request.UpdateEventRequest;
import com.DTPM.FlashSale.dto.response.EventResponse;
import com.DTPM.FlashSale.entity.Event;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", uses = {TicketTypeMapper.class})
public interface EventMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", expression = "java(com.DTPM.FlashSale.enums.EventStatus.DRAFT)")
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "organizer", ignore = true)
    @Mapping(target = "ticketTypes", ignore = true)
    Event toEntity(CreateEventRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "organizer", ignore = true)
    @Mapping(target = "ticketTypes", ignore = true)
    void updateEntityFromRequest(UpdateEventRequest request, @MappingTarget Event event);

    @Mapping(target = "organizerId", source = "organizer.id")
    @Mapping(target = "organizerName", source = "organizer.fullName")
    EventResponse toEventResponse(Event event);
}
