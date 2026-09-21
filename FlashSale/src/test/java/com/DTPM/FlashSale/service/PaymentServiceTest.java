package com.DTPM.FlashSale.service;

import com.DTPM.FlashSale.dto.response.PaymentResponse;
import com.DTPM.FlashSale.entity.Order;
import com.DTPM.FlashSale.entity.Payment;
import com.DTPM.FlashSale.entity.User;
import com.DTPM.FlashSale.enums.OrderStatus;
import com.DTPM.FlashSale.enums.PaymentStatus;
import com.DTPM.FlashSale.exception.AppException;
import com.DTPM.FlashSale.exception.ErrorCode;
import com.DTPM.FlashSale.infrastructure.QrCodeService;
import com.DTPM.FlashSale.mapper.PaymentMapper;
import com.DTPM.FlashSale.repository.OrderRepository;
import com.DTPM.FlashSale.repository.PaymentRepository;
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
class PaymentServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private QrCodeService qrCodeService;

    @Mock
    private PaymentMapper paymentMapper;

    @InjectMocks
    private PaymentService paymentService;

    private User user;
    private Order pendingOrder;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).email("user1@example.com").build();

        pendingOrder = Order.builder()
                .id(10L)
                .orderCode("FS-PAY-123")
                .user(user)
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.valueOf(500))
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();
    }

    @Test
    @DisplayName("pay: Thất bại khi không tìm thấy Order (ORDER_NOT_FOUND)")
    void pay_OrderNotFound() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> paymentService.pay(1L, 999L));
        assertEquals(ErrorCode.ORDER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("pay: Thất bại khi không phải chủ đơn (ORDER_ACCESS_DENIED)")
    void pay_AccessDenied() {
        when(orderRepository.findById(10L)).thenReturn(Optional.of(pendingOrder));

        AppException exception = assertThrows(AppException.class, () -> paymentService.pay(2L, 10L));
        assertEquals(ErrorCode.ORDER_ACCESS_DENIED, exception.getErrorCode());
    }

    @Test
    @DisplayName("pay: Thất bại khi đơn đã được thanh toán trước đó (PAYMENT_ALREADY_EXISTS)")
    void pay_AlreadyPaid() {
        when(orderRepository.findById(10L)).thenReturn(Optional.of(pendingOrder));
        when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.of(Payment.builder().id(100L).build()));

        AppException exception = assertThrows(AppException.class, () -> paymentService.pay(1L, 10L));
        assertEquals(ErrorCode.PAYMENT_ALREADY_EXISTS, exception.getErrorCode());
    }

    @Test
    @DisplayName("pay: Thất bại khi đơn không ở trạng thái PENDING (ORDER_NOT_PENDING)")
    void pay_NotPending() {
        pendingOrder.setStatus(OrderStatus.CANCELLED);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(pendingOrder));
        when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> paymentService.pay(1L, 10L));
        assertEquals(ErrorCode.ORDER_NOT_PENDING, exception.getErrorCode());
    }

    @Test
    @DisplayName("pay: Thất bại khi đơn đã hết hạn (ORDER_EXPIRED)")
    void pay_OrderExpired() {
        pendingOrder.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(orderRepository.findById(10L)).thenReturn(Optional.of(pendingOrder));
        when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> paymentService.pay(1L, 10L));
        assertEquals(ErrorCode.ORDER_EXPIRED, exception.getErrorCode());
    }

    @Test
    @DisplayName("pay: Thành công thanh toán Mock và chuyển trạng thái PAID")
    void pay_Success() {
        when(orderRepository.findById(10L)).thenReturn(Optional.of(pendingOrder));
        when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        Payment payment = Payment.createSuccess(pendingOrder, pendingOrder.getTotalAmount());
        payment.setId(100L);

        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(qrCodeService.generate("FS-PAY-123")).thenReturn("https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=PAY-FS-PAY-123");

        PaymentResponse mockResponse = PaymentResponse.builder()
                .id(100L)
                .orderId(10L)
                .orderCode("FS-PAY-123")
                .amount(BigDecimal.valueOf(500))
                .paymentStatus(PaymentStatus.SUCCESS)
                .orderStatus(OrderStatus.PAID)
                .paidAt(payment.getPaidAt())
                .build();

        when(paymentMapper.toPaymentResponse(any(Payment.class))).thenReturn(mockResponse);

        PaymentResponse response = paymentService.pay(1L, 10L);

        assertNotNull(response);
        assertEquals(OrderStatus.PAID, pendingOrder.getStatus());
        assertEquals(PaymentStatus.SUCCESS, response.getPaymentStatus());
        assertNotNull(response.getQrCodeUrl());

        verify(orderRepository, times(1)).save(pendingOrder);
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }
}
