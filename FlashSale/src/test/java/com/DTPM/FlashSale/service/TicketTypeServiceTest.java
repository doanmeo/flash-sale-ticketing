package com.DTPM.FlashSale.service;

import com.DTPM.FlashSale.dto.request.CreateTicketTypeRequest;
import com.DTPM.FlashSale.dto.response.TicketTypeResponse;
import com.DTPM.FlashSale.entity.Event;
import com.DTPM.FlashSale.entity.TicketType;
import com.DTPM.FlashSale.entity.User;
import com.DTPM.FlashSale.enums.UserRole;
import com.DTPM.FlashSale.infrastructure.RedisStockService;
import com.DTPM.FlashSale.mapper.TicketTypeMapper;
import com.DTPM.FlashSale.repository.EventRepository;
import com.DTPM.FlashSale.repository.TicketTypeRepository;
import com.DTPM.FlashSale.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketTypeServiceTest {

    @Mock
    private TicketTypeRepository ticketTypeRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RedisStockService redisStockService;

    @Mock
    private TicketTypeMapper ticketTypeMapper;

    @InjectMocks
    private TicketTypeService ticketTypeService;

    private User organizer;
    private Event event;
    private TicketType ticketType;

    @BeforeEach
    void setUp() {
        organizer = User.builder().id(1L).email("org@example.com").role(UserRole.ORGANIZER).build();

        event = Event.builder()
                .id(10L)
                .name("Rock Concert 2026")
                .organizer(organizer)
                .build();

        ticketType = TicketType.builder()
                .id(100L)
                .name("VIP")
                .price(BigDecimal.valueOf(200))
                .totalQuantity(50)
                .availableQuantity(50)
                .event(event)
                .build();
    }

    @Test
    @DisplayName("createTicketType: Thành công tạo loại vé và khởi tạo Redis stock")
    void createTicketType_Success() {
        CreateTicketTypeRequest request = CreateTicketTypeRequest.builder()
                .name("VIP")
                .price(BigDecimal.valueOf(200))
                .totalQuantity(50)
                .saleStart(LocalDateTime.now().minusHours(1))
                .saleEnd(LocalDateTime.now().plusDays(5))
                .minPerOrder(1)
                .maxPerOrder(10)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(organizer));
        when(eventRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(event));
        when(ticketTypeMapper.toEntity(request)).thenReturn(ticketType);
        when(ticketTypeRepository.save(any(TicketType.class))).thenReturn(ticketType);
        when(ticketTypeMapper.toTicketTypeResponse(ticketType)).thenReturn(TicketTypeResponse.builder().id(100L).name("VIP").totalQuantity(50).build());
        when(redisStockService.getAvailable(100L)).thenReturn(50);

        TicketTypeResponse response = ticketTypeService.createTicketType(1L, 10L, request);

        assertNotNull(response);
        assertEquals(50, response.getRealtimeAvailable());
        verify(redisStockService, times(1)).initStock(100L, 50);
        verify(ticketTypeRepository, times(1)).save(ticketType);
    }
}
