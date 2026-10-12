package com.DTPM.FlashSale.controller;

import com.DTPM.FlashSale.dto.response.OrderResponse;
import com.DTPM.FlashSale.domain.enums.OrderStatus;
import com.DTPM.FlashSale.domain.enums.UserRole;
import com.DTPM.FlashSale.domain.enums.UserStatus;
import com.DTPM.FlashSale.exception.AppException;
import com.DTPM.FlashSale.exception.ErrorCode;
import com.DTPM.FlashSale.exception.GlobalExceptionHandler;
import com.DTPM.FlashSale.security.UserPrincipal;
import com.DTPM.FlashSale.service.BookingService;
import com.DTPM.FlashSale.service.OrderService;
import com.DTPM.FlashSale.service.PaymentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

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

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private OrderController orderController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(orderController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateUser(Long userId) {
        UserPrincipal principal = UserPrincipal.builder()
                .id(userId)
                .email("user" + userId + "@example.com")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .authorities(Collections.emptyList())
                .build();

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    @DisplayName("POST /api/orders/{orderId}/cancel - Hủy đơn thành công trả về 200 OK")
    void cancelOrder_Success() throws Exception {
        authenticateUser(1L);

        OrderResponse response = OrderResponse.builder()
                .id(10L)
                .orderCode("FS-CANCEL-123")
                .status(OrderStatus.CANCELLED)
                .totalAmount(BigDecimal.valueOf(150))
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .createdAt(LocalDateTime.now())
                .build();

        when(orderService.cancel(eq(1L), eq(10L))).thenReturn(response);

        mockMvc.perform(post("/api/orders/10/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.orderCode").value("FS-CANCEL-123"))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("POST /api/orders/{orderId}/cancel - Trả về 403 FORBIDDEN khi không phải chủ đơn")
    void cancelOrder_AccessDenied() throws Exception {
        authenticateUser(2L);

        when(orderService.cancel(eq(2L), eq(10L)))
                .thenThrow(new AppException(ErrorCode.ORDER_ACCESS_DENIED));

        mockMvc.perform(post("/api/orders/10/cancel"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(1506))
                .andExpect(jsonPath("$.message").value("You do not own this order"));
    }

    @Test
    @DisplayName("POST /api/orders/{orderId}/cancel - Trả về 404 NOT_FOUND khi không tìm thấy đơn")
    void cancelOrder_NotFound() throws Exception {
        authenticateUser(1L);

        when(orderService.cancel(eq(1L), eq(999L)))
                .thenThrow(new AppException(ErrorCode.ORDER_NOT_FOUND));

        mockMvc.perform(post("/api/orders/999/cancel"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(1500))
                .andExpect(jsonPath("$.message").value("Order not found"));
    }
}
