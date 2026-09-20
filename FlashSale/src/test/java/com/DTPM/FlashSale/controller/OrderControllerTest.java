package com.DTPM.FlashSale.controller;

import com.DTPM.FlashSale.dto.response.OrderResponse;
import com.DTPM.FlashSale.enums.OrderStatus;
import com.DTPM.FlashSale.exception.AppException;
import com.DTPM.FlashSale.exception.ErrorCode;
import com.DTPM.FlashSale.exception.GlobalExceptionHandler;
import com.DTPM.FlashSale.service.BookingService;
import com.DTPM.FlashSale.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    private MockMvc mockMvc;

    @Mock
    private BookingService bookingService;

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController orderController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(orderController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/orders/{orderId}/cancel - Hủy đơn thành công trả về 200 OK")
    void cancelOrder_Success() throws Exception {
        OrderResponse response = OrderResponse.builder()
                .id(10L)
                .orderCode("FS-CANCEL-123")
                .status(OrderStatus.CANCELLED)
                .totalAmount(BigDecimal.valueOf(150))
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .createdAt(LocalDateTime.now())
                .build();

        when(orderService.cancel(eq(1L), eq(10L))).thenReturn(response);

        mockMvc.perform(post("/api/orders/10/cancel")
                        .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.orderCode").value("FS-CANCEL-123"))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("POST /api/orders/{orderId}/cancel - Trả về 403 FORBIDDEN khi không phải chủ đơn")
    void cancelOrder_AccessDenied() throws Exception {
        when(orderService.cancel(eq(2L), eq(10L)))
                .thenThrow(new AppException(ErrorCode.ORDER_ACCESS_DENIED));

        mockMvc.perform(post("/api/orders/10/cancel")
                        .header("X-User-Id", 2L))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(1506))
                .andExpect(jsonPath("$.message").value("You do not own this order"));
    }

    @Test
    @DisplayName("POST /api/orders/{orderId}/cancel - Trả về 404 NOT_FOUND khi không tìm thấy đơn")
    void cancelOrder_NotFound() throws Exception {
        when(orderService.cancel(eq(1L), eq(999L)))
                .thenThrow(new AppException(ErrorCode.ORDER_NOT_FOUND));

        mockMvc.perform(post("/api/orders/999/cancel")
                        .header("X-User-Id", 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(1500))
                .andExpect(jsonPath("$.message").value("Order not found"));
    }
}
