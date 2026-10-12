package com.DTPM.FlashSale.service;

import com.DTPM.FlashSale.dto.request.CreateEventRequest;
import com.DTPM.FlashSale.dto.request.UpdateEventRequest;
import com.DTPM.FlashSale.dto.response.EventResponse;
import com.DTPM.FlashSale.domain.entity.Event;
import com.DTPM.FlashSale.domain.entity.TicketType;
import com.DTPM.FlashSale.domain.entity.User;
import com.DTPM.FlashSale.domain.enums.EventStatus;
import com.DTPM.FlashSale.domain.enums.UserRole;
import com.DTPM.FlashSale.exception.AppException;
import com.DTPM.FlashSale.exception.ErrorCode;
import com.DTPM.FlashSale.infrastructure.RedisStockService;
import com.DTPM.FlashSale.mapper.EventMapper;
import com.DTPM.FlashSale.domain.repository.EventRepository;
import com.DTPM.FlashSale.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventMapper eventMapper;

    @Mock
    private RedisStockService redisStockService;

    @InjectMocks
    private EventService eventService;

    private User organizer;
    private User otherUser;
    private Event event;

    @BeforeEach
    void setUp() {
        organizer = User.builder().id(1L).email("org@example.com").role(UserRole.ORGANIZER).build();
        otherUser = User.builder().id(2L).email("other@example.com").role(UserRole.ORGANIZER).build();

        event = Event.builder()
                .id(10L)
                .name("Rock Concert 2026")
                .location("Hanoi Stadium")
                .eventDate(LocalDateTime.now().plusDays(10))
                .status(EventStatus.DRAFT)
                .organizer(organizer)
                .ticketTypes(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("createEvent: Tạo event thành công với trạng thái DRAFT")
    void createEvent_Success() {
        CreateEventRequest request = CreateEventRequest.builder()
                .name("Rock Concert 2026")
                .location("Hanoi Stadium")
                .eventDate(LocalDateTime.now().plusDays(10))
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(organizer));
        when(eventMapper.toEntity(request)).thenReturn(event);
        when(eventRepository.save(any(Event.class))).thenReturn(event);
        when(eventMapper.toEventResponse(event)).thenReturn(EventResponse.builder().id(10L).name("Rock Concert 2026").status(EventStatus.DRAFT).build());

        EventResponse response = eventService.createEvent(1L, request);

        assertNotNull(response);
        assertEquals(EventStatus.DRAFT, response.getStatus());
        verify(eventRepository, times(1)).save(event);
    }

    @Test
    @DisplayName("updateEvent: Thất bại khi không phải organizer của event (EVENT_ACCESS_DENIED)")
    void updateEvent_AccessDenied() {
        UpdateEventRequest request = UpdateEventRequest.builder().name("New Name").build();
        when(userRepository.findById(2L)).thenReturn(Optional.of(otherUser));
        when(eventRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(event));

        AppException exception = assertThrows(AppException.class, () -> eventService.updateEvent(2L, 10L, request));
        assertEquals(ErrorCode.EVENT_ACCESS_DENIED, exception.getErrorCode());
    }

    @Test
    @DisplayName("publishEvent: Thất bại khi event chưa có loại vé nào")
    void publishEvent_NoTicketType() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(organizer));
        when(eventRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(event));

        AppException exception = assertThrows(AppException.class, () -> eventService.publishEvent(1L, 10L));
        assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());
    }

    @Test
    @DisplayName("publishEvent: Thành công chuyển trạng thái sang PUBLISHED khi đã có vé")
    void publishEvent_Success() {
        event.getTicketTypes().add(TicketType.builder().id(100L).build());
        when(userRepository.findById(1L)).thenReturn(Optional.of(organizer));
        when(eventRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenReturn(event);
        when(eventMapper.toEventResponse(event)).thenReturn(EventResponse.builder().id(10L).status(EventStatus.PUBLISHED).build());

        EventResponse response = eventService.publishEvent(1L, 10L);

        assertNotNull(response);
        assertEquals(EventStatus.PUBLISHED, event.getStatus());
        verify(eventRepository, times(1)).save(event);
    }
}
