package com.DTPM.FlashSale.service;

import com.DTPM.FlashSale.dto.request.CreateEventRequest;
import com.DTPM.FlashSale.dto.request.UpdateEventRequest;
import com.DTPM.FlashSale.dto.response.EventResponse;
import com.DTPM.FlashSale.dto.response.TicketTypeResponse;
import com.DTPM.FlashSale.entity.Event;
import com.DTPM.FlashSale.entity.User;
import com.DTPM.FlashSale.enums.EventStatus;
import com.DTPM.FlashSale.exception.AppException;
import com.DTPM.FlashSale.exception.ErrorCode;
import com.DTPM.FlashSale.infrastructure.RedisStockService;
import com.DTPM.FlashSale.mapper.EventMapper;
import com.DTPM.FlashSale.repository.EventRepository;
import com.DTPM.FlashSale.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final EventMapper eventMapper;
    private final RedisStockService redisStockService;

    @Transactional
    public EventResponse createEvent(Long organizerId, CreateEventRequest request) {
        User organizer = userRepository.findById(organizerId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Event event = eventMapper.toEntity(request);
        event.setOrganizer(organizer);
        event.setStatus(EventStatus.DRAFT);

        Event saved = eventRepository.save(event);
        log.info("Event id={} created by organizer id={}", saved.getId(), organizerId);
        return eventMapper.toEventResponse(saved);
    }

    @Transactional
    public EventResponse updateEvent(Long organizerId, Long eventId, UpdateEventRequest request) {
        User organizer = userRepository.findById(organizerId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Event event = eventRepository.findByIdAndDeletedAtIsNull(eventId)
                .orElseThrow(() -> new AppException(ErrorCode.EVENT_NOT_FOUND));

        if (!event.isOwnedBy(organizer)) {
            throw new AppException(ErrorCode.EVENT_ACCESS_DENIED);
        }

        if (!event.canBeModified()) {
            throw new AppException(ErrorCode.EVENT_NOT_MODIFIABLE);
        }

        eventMapper.updateEntityFromRequest(request, event);
        Event updated = eventRepository.save(event);

        return enrichEventResponse(eventMapper.toEventResponse(updated));
    }

    @Transactional
    public EventResponse publishEvent(Long organizerId, Long eventId) {
        User organizer = userRepository.findById(organizerId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Event event = eventRepository.findByIdAndDeletedAtIsNull(eventId)
                .orElseThrow(() -> new AppException(ErrorCode.EVENT_NOT_FOUND));

        if (!event.isOwnedBy(organizer)) {
            throw new AppException(ErrorCode.EVENT_ACCESS_DENIED);
        }

        if (!event.canBeModified()) {
            throw new AppException(ErrorCode.EVENT_NOT_MODIFIABLE);
        }

        if (event.getTicketTypes() == null || event.getTicketTypes().stream().noneMatch(t -> t.getDeletedAt() == null)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Event must have at least one active ticket type before publishing");
        }

        event.publish();
        Event saved = eventRepository.save(event);
        log.info("Event id={} published by organizer id={}", eventId, organizerId);

        return enrichEventResponse(eventMapper.toEventResponse(saved));
    }

    @Transactional
    public void softDeleteEvent(Long organizerId, Long eventId) {
        User organizer = userRepository.findById(organizerId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Event event = eventRepository.findByIdAndDeletedAtIsNull(eventId)
                .orElseThrow(() -> new AppException(ErrorCode.EVENT_NOT_FOUND));

        if (!event.isOwnedBy(organizer)) {
            throw new AppException(ErrorCode.EVENT_ACCESS_DENIED);
        }

        event.softDelete();
        eventRepository.save(event);
        log.info("Event id={} soft deleted by organizer id={}", eventId, organizerId);
    }

    @Transactional(readOnly = true)
    public Page<EventResponse> getPublishedEvents(String keyword, Pageable pageable) {
        Page<Event> page;
        if (keyword != null && !keyword.isBlank()) {
            page = eventRepository.findByNameContainingIgnoreCaseAndStatusAndDeletedAtIsNull(
                    keyword.trim(), EventStatus.PUBLISHED, pageable);
        } else {
            page = eventRepository.findByStatusAndDeletedAtIsNull(EventStatus.PUBLISHED, pageable);
        }

        return page.map(eventMapper::toEventResponse).map(this::enrichEventResponse);
    }

    @Transactional(readOnly = true)
    public EventResponse getEventDetail(Long eventId) {
        Event event = eventRepository.findByIdAndDeletedAtIsNull(eventId)
                .orElseThrow(() -> new AppException(ErrorCode.EVENT_NOT_FOUND));

        return enrichEventResponse(eventMapper.toEventResponse(event));
    }

    @Transactional(readOnly = true)
    public Page<EventResponse> getOrganizerEvents(Long organizerId, Pageable pageable) {
        return eventRepository.findByOrganizerIdAndDeletedAtIsNull(organizerId, pageable)
                .map(eventMapper::toEventResponse)
                .map(this::enrichEventResponse);
    }

    @Transactional(readOnly = true)
    public Page<EventResponse> getAdminEvents(String keyword, Pageable pageable) {
        Page<Event> page;
        if (keyword != null && !keyword.isBlank()) {
            page = eventRepository.findByNameContainingIgnoreCaseAndDeletedAtIsNull(keyword.trim(), pageable);
        } else {
            page = eventRepository.findByDeletedAtIsNull(pageable);
        }
        return page.map(eventMapper::toEventResponse).map(this::enrichEventResponse);
    }

    @Transactional
    public EventResponse adminUpdateEvent(Long eventId, UpdateEventRequest request) {
        Event event = eventRepository.findByIdAndDeletedAtIsNull(eventId)
                .orElseThrow(() -> new AppException(ErrorCode.EVENT_NOT_FOUND));

        eventMapper.updateEntityFromRequest(request, event);
        Event updated = eventRepository.save(event);
        log.info("Event id={} updated by admin", eventId);

        return enrichEventResponse(eventMapper.toEventResponse(updated));
    }

    @Transactional
    public void adminDeleteEvent(Long eventId) {
        Event event = eventRepository.findByIdAndDeletedAtIsNull(eventId)
                .orElseThrow(() -> new AppException(ErrorCode.EVENT_NOT_FOUND));

        event.softDelete();
        eventRepository.save(event);
        log.info("Event id={} soft deleted by admin", eventId);
    }

    private EventResponse enrichEventResponse(EventResponse response) {
        if (response.getTicketTypes() != null) {
            List<TicketTypeResponse> enrichedTypes = response.getTicketTypes().stream()
                    .filter(t -> Boolean.FALSE.equals(t.getIsHidden()))
                    .peek(t -> t.setRealtimeAvailable(redisStockService.getAvailable(t.getId())))
                    .toList();
            response.setTicketTypes(enrichedTypes);
        }
        return response;
    }
}

