package com.DTPM.FlashSale.service;

import com.DTPM.FlashSale.dto.response.OrderResponse;
import com.DTPM.FlashSale.entity.Order;
import com.DTPM.FlashSale.entity.OrderItem;
import com.DTPM.FlashSale.entity.TicketType;
import com.DTPM.FlashSale.entity.User;
import com.DTPM.FlashSale.enums.OrderStatus;
import com.DTPM.FlashSale.exception.AppException;
import com.DTPM.FlashSale.exception.ErrorCode;
import com.DTPM.FlashSale.infrastructure.RedisStockService;
import com.DTPM.FlashSale.mapper.OrderMapper;
import com.DTPM.FlashSale.repository.OrderRepository;
import com.DTPM.FlashSale.repository.TicketTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private TicketTypeRepository ticketTypeRepository;

    @Mock
    private RedisStockService redisStockService;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderService orderService;

    private User user;
    private TicketType ticketType;
    private Order order;
    private OrderItem orderItem;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).email("user1@example.com").build();

        ticketType = TicketType.builder()
                .id(100L)
                .name("VIP")
                .availableQuantity(10)
                .price(BigDecimal.valueOf(100))
                .build();

        orderItem = OrderItem.builder()
                .id(10L)
                .ticketType(ticketType)
                .quantity(2)
                .unitPrice(BigDecimal.valueOf(100))
                .build();

        order = Order.builder()
                .id(50L)
                .orderCode("FS-TEST-001")
                .user(user)
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.valueOf(200))
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .items(new ArrayList<>())
                .build();
        order.addItem(orderItem);
    }

    @Test
    @DisplayName("cancel: Thất bại khi không tìm thấy Order (ORDER_NOT_FOUND)")
    void cancel_OrderNotFound() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class,
                () -> orderService.cancel(1L, 999L));

        assertEquals(ErrorCode.ORDER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("cancel: Thất bại khi không phải chủ đơn (ORDER_ACCESS_DENIED)")
    void cancel_AccessDenied() {
        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));

        AppException exception = assertThrows(AppException.class,
                () -> orderService.cancel(2L, 50L));

        assertEquals(ErrorCode.ORDER_ACCESS_DENIED, exception.getErrorCode());
    }

    @Test
    @DisplayName("cancel: Thất bại khi đơn đã hết hạn (ORDER_EXPIRED)")
    void cancel_OrderExpired() {
        order.setExpiresAt(LocalDateTime.now().minusMinutes(1)); // Quá hạn hold
        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));

        AppException exception = assertThrows(AppException.class,
                () -> orderService.cancel(1L, 50L));

        assertEquals(ErrorCode.ORDER_EXPIRED, exception.getErrorCode());
    }

    @Test
    @DisplayName("cancel: Thất bại khi trạng thái là PAID (ORDER_CANNOT_CANCEL)")
    void cancel_OrderPaid() {
        order.setStatus(OrderStatus.PAID);
        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));

        AppException exception = assertThrows(AppException.class,
                () -> orderService.cancel(1L, 50L));

        assertEquals(ErrorCode.ORDER_CANNOT_CANCEL, exception.getErrorCode());
    }

    @Test
    @DisplayName("cancel: Thành công hủy đơn PENDING và hoàn trả stock")
    void cancel_Success() {
        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        OrderResponse mockResponse = OrderResponse.builder()
                .id(50L)
                .orderCode("FS-TEST-001")
                .status(OrderStatus.CANCELLED)
                .build();
        when(orderMapper.toOrderResponse(any(Order.class))).thenReturn(mockResponse);

        OrderResponse response = orderService.cancel(1L, 50L);

        assertNotNull(response);
        assertEquals(OrderStatus.CANCELLED, response.getStatus());
        assertEquals(12, ticketType.getAvailableQuantity()); // 10 + 2

        verify(redisStockService, times(1)).increase(100L, 2);
        verify(ticketTypeRepository, times(1)).save(ticketType);
        verify(orderRepository, times(1)).save(order);
    }

    @Test
    @DisplayName("expire: Bỏ qua (idempotent) khi order không tồn tại hoặc không ở PENDING")
    void expire_Idempotent() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());
        orderService.expire(99L);

        order.setStatus(OrderStatus.PAID);
        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));
        orderService.expire(50L);

        verify(redisStockService, never()).increase(anyLong(), anyInt());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("expire: Thành công chuyển EXPIRED và hoàn trả stock")
    void expire_Success() {
        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        orderService.expire(50L);

        assertEquals(OrderStatus.EXPIRED, order.getStatus());
        assertEquals(12, ticketType.getAvailableQuantity());

        verify(redisStockService, times(1)).increase(100L, 2);
        verify(ticketTypeRepository, times(1)).save(ticketType);
        verify(orderRepository, times(1)).save(order);
    }
}
