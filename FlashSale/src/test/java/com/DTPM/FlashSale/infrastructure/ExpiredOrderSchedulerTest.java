package com.DTPM.FlashSale.infrastructure;

import com.DTPM.FlashSale.entity.Order;
import com.DTPM.FlashSale.enums.OrderStatus;
import com.DTPM.FlashSale.repository.OrderRepository;
import com.DTPM.FlashSale.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpiredOrderSchedulerTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderService orderService;

    @InjectMocks
    private ExpiredOrderScheduler scheduler;

    @Test
    @DisplayName("processExpiredOrders: Không làm gì khi không có đơn hết hạn")
    void processExpiredOrders_Empty() {
        when(orderRepository.findByStatusAndExpiresAtBefore(eq(OrderStatus.PENDING), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        scheduler.processExpiredOrders();

        verify(orderService, never()).expire(anyLong());
    }

    @Test
    @DisplayName("processExpiredOrders: Hết hạn từng đơn và không dừng cả job khi 1 đơn bị lỗi")
    void processExpiredOrders_ProcessEachAndHandleError() {
        Order order1 = Order.builder().id(101L).build();
        Order order2 = Order.builder().id(102L).build();

        when(orderRepository.findByStatusAndExpiresAtBefore(eq(OrderStatus.PENDING), any(LocalDateTime.class)))
                .thenReturn(List.of(order1, order2));

        doThrow(new RuntimeException("DB Exception")).when(orderService).expire(101L);
        doNothing().when(orderService).expire(102L);

        scheduler.processExpiredOrders();

        verify(orderService, times(1)).expire(101L);
        verify(orderService, times(1)).expire(102L);
    }
}
