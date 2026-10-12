package com.DTPM.FlashSale.service;

import com.DTPM.FlashSale.domain.entity.*;
import com.DTPM.FlashSale.domain.enums.EventStatus;
import com.DTPM.FlashSale.domain.enums.LineItemType;
import com.DTPM.FlashSale.domain.enums.OrderStatus;
import com.DTPM.FlashSale.domain.enums.SeatStatus;
import com.DTPM.FlashSale.domain.enums.UserStatus;
import com.DTPM.FlashSale.domain.repository.*;
import com.DTPM.FlashSale.dto.request.CreateOrderRequest;
import com.DTPM.FlashSale.dto.request.CreateSeatedOrderRequest;
import com.DTPM.FlashSale.dto.response.OrderResponse;
import com.DTPM.FlashSale.exception.AppException;
import com.DTPM.FlashSale.exception.ErrorCode;
import com.DTPM.FlashSale.infrastructure.RedisSeatLockService;
import com.DTPM.FlashSale.infrastructure.RedisStockService;
import com.DTPM.FlashSale.mapper.OrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private TicketTypeRepository ticketTypeRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RedisStockService redisStockService;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private ShowSessionRepository showSessionRepository;

    @Mock
    private SessionSeatRepository sessionSeatRepository;

    @Mock
    private RedisSeatLockService redisSeatLockService;

    @Mock
    private FlashsaleItemRepository flashsaleItemRepository;

    @InjectMocks
    private BookingService bookingService;

    private User user;
    private Event event;
    private TicketType ticketType;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(bookingService, "holdMinutes", 10);

        user = User.builder()
                .id(1L)
                .email("user1@example.com")
                .status(UserStatus.ACTIVE)
                .build();

        event = Event.builder()
                .id(10L)
                .name("Test Concert")
                .status(EventStatus.PUBLISHED)
                .build();

        ticketType = TicketType.builder()
                .id(100L)
                .name("VIP Ticket")
                .price(BigDecimal.valueOf(1000))
                .totalQuantity(50)
                .availableQuantity(50)
                .saleStart(LocalDateTime.now().minusHours(1))
                .saleEnd(LocalDateTime.now().plusHours(5))
                .minPerOrder(1)
                .maxPerOrder(4)
                .isHidden(false)
                .event(event)
                .build();
    }

    @Test
    @DisplayName("createOrder GA: Đặt vé thành công với giá gốc khi không có flash sale")
    void createOrder_GaSuccess() {
        CreateOrderRequest request = new CreateOrderRequest(100L, 2);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(ticketTypeRepository.findByIdAndDeletedAtIsNull(100L)).thenReturn(Optional.of(ticketType));
        when(redisStockService.atomicDecrease(100L, 2)).thenReturn(true);
        when(flashsaleItemRepository.findActiveFlashSaleItem(eq(100L), any(LocalDateTime.class))).thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse mockResponse = OrderResponse.builder()
                .id(1L)
                .orderCode("FS-TEST-001")
                .totalAmount(BigDecimal.valueOf(2000))
                .status(OrderStatus.PENDING)
                .build();
        when(orderMapper.toOrderResponse(any(Order.class))).thenReturn(mockResponse);

        OrderResponse response = bookingService.createOrder(1L, request);

        assertNotNull(response);
        assertEquals(OrderStatus.PENDING, response.getStatus());
        assertEquals(BigDecimal.valueOf(2000), response.getTotalAmount());
        verify(redisStockService).atomicDecrease(100L, 2);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("createOrder GA: Áp dụng giá Flash Sale khi có campaign hoạt động")
    void createOrder_GaFlashSalePrice() {
        CreateOrderRequest request = new CreateOrderRequest(100L, 2);

        FlashsaleItem flashItem = FlashsaleItem.builder()
                .id(1L)
                .flashPrice(BigDecimal.valueOf(800))
                .flashQuota(10)
                .flashAvailable(10)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(ticketTypeRepository.findByIdAndDeletedAtIsNull(100L)).thenReturn(Optional.of(ticketType));
        when(redisStockService.atomicDecrease(100L, 2)).thenReturn(true);
        when(flashsaleItemRepository.findActiveFlashSaleItem(eq(100L), any(LocalDateTime.class))).thenReturn(Optional.of(flashItem));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse mockResponse = OrderResponse.builder()
                .id(1L)
                .orderCode("FS-TEST-002")
                .totalAmount(BigDecimal.valueOf(1600))
                .status(OrderStatus.PENDING)
                .build();
        when(orderMapper.toOrderResponse(any(Order.class))).thenReturn(mockResponse);

        OrderResponse response = bookingService.createOrder(1L, request);

        assertNotNull(response);
        assertEquals(BigDecimal.valueOf(1600), response.getTotalAmount());
        assertEquals(8, flashItem.getFlashAvailable()); // 10 - 2 = 8
        verify(flashsaleItemRepository).save(flashItem);
    }

    @Test
    @DisplayName("createOrder GA: Ném ngoại lệ SOLD_OUT khi Redis hết vé")
    void createOrder_SoldOut() {
        CreateOrderRequest request = new CreateOrderRequest(100L, 2);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(ticketTypeRepository.findByIdAndDeletedAtIsNull(100L)).thenReturn(Optional.of(ticketType));
        when(redisStockService.atomicDecrease(100L, 2)).thenReturn(false);

        AppException ex = assertThrows(AppException.class, () -> bookingService.createOrder(1L, request));
        assertEquals(ErrorCode.SOLD_OUT, ex.getErrorCode());
    }

    @Test
    @DisplayName("createOrderSeated: Đặt vé có chỗ ngồi thành công")
    void createOrderSeated_Success() {
        CreateSeatedOrderRequest request = new CreateSeatedOrderRequest(5L, List.of(201L, 202L));

        ShowSession session = ShowSession.builder()
                .id(5L)
                .sessionName("Evening Session")
                .event(event)
                .build();

        Zone zone = Zone.builder().id(50L).zoneName("Zone A").isSeated(true).build();
        Seat seat1 = Seat.builder().id(201L).rowLabel("A").seatNumber("1").zone(zone).build();
        Seat seat2 = Seat.builder().id(202L).rowLabel("A").seatNumber("2").zone(zone).build();

        SessionSeat ss1 = SessionSeat.builder().id(301L).showSession(session).seat(seat1).seatStatus(SeatStatus.AVAILABLE).build();
        SessionSeat ss2 = SessionSeat.builder().id(302L).showSession(session).seat(seat2).seatStatus(SeatStatus.AVAILABLE).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(showSessionRepository.findById(5L)).thenReturn(Optional.of(session));
        when(sessionSeatRepository.findByShowSessionIdAndSeatIdIn(5L, List.of(201L, 202L))).thenReturn(List.of(ss1, ss2));
        when(redisSeatLockService.isSeatLocked(5L, 201L)).thenReturn(false);
        when(redisSeatLockService.isSeatLocked(5L, 202L)).thenReturn(false);
        when(redisSeatLockService.acquireMultipleSeatLocks(5L, List.of(201L, 202L), 1L, 600L)).thenReturn(true);

        when(ticketTypeRepository.findFirstByShowSessionIdAndZoneIdAndDeletedAtIsNull(5L, 50L)).thenReturn(Optional.of(ticketType));
        when(flashsaleItemRepository.findActiveFlashSaleItem(eq(100L), any(LocalDateTime.class))).thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse mockResponse = OrderResponse.builder()
                .id(1L)
                .orderCode("FS-SEAT-001")
                .totalAmount(BigDecimal.valueOf(2000))
                .status(OrderStatus.PENDING)
                .build();
        when(orderMapper.toOrderResponse(any(Order.class))).thenReturn(mockResponse);

        OrderResponse response = bookingService.createOrderSeated(1L, request);

        assertNotNull(response);
        assertEquals(SeatStatus.LOCKED, ss1.getSeatStatus());
        assertEquals(SeatStatus.LOCKED, ss2.getSeatStatus());
        verify(sessionSeatRepository).save(ss1);
        verify(sessionSeatRepository).save(ss2);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("createOrderSeated: Ném SEAT_LOCKED khi ghế đang bị người khác giữ")
    void createOrderSeated_SeatLocked() {
        CreateSeatedOrderRequest request = new CreateSeatedOrderRequest(5L, List.of(201L));

        ShowSession session = ShowSession.builder()
                .id(5L)
                .sessionName("Evening Session")
                .event(event)
                .build();

        Zone zone = Zone.builder().id(50L).zoneName("Zone A").isSeated(true).build();
        Seat seat1 = Seat.builder().id(201L).rowLabel("A").seatNumber("1").zone(zone).build();
        SessionSeat ss1 = SessionSeat.builder().id(301L).showSession(session).seat(seat1).seatStatus(SeatStatus.LOCKED).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(showSessionRepository.findById(5L)).thenReturn(Optional.of(session));
        when(sessionSeatRepository.findByShowSessionIdAndSeatIdIn(5L, List.of(201L))).thenReturn(List.of(ss1));

        AppException ex = assertThrows(AppException.class, () -> bookingService.createOrderSeated(1L, request));
        assertEquals(ErrorCode.SEAT_LOCKED, ex.getErrorCode());
    }
}
