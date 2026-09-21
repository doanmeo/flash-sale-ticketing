package com.DTPM.FlashSale.controller;

import com.DTPM.FlashSale.dto.request.CreateOrderRequest;
import com.DTPM.FlashSale.dto.response.ApiResponse;
import com.DTPM.FlashSale.dto.response.OrderResponse;
import com.DTPM.FlashSale.dto.response.PaymentResponse;
import com.DTPM.FlashSale.security.SecurityUtils;
import com.DTPM.FlashSale.service.BookingService;
import com.DTPM.FlashSale.service.OrderService;
import com.DTPM.FlashSale.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final BookingService bookingService;
    private final OrderService orderService;
    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        OrderResponse response = bookingService.createOrder(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(@PathVariable Long orderId) {
        Long userId = SecurityUtils.getCurrentUserId();
        OrderResponse response = orderService.cancel(userId, orderId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{orderId}/pay")
    public ResponseEntity<ApiResponse<PaymentResponse>> payOrder(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @PathVariable Long orderId) {
        Long userId = headerUserId != null ? headerUserId : SecurityUtils.getCurrentUserId();
        PaymentResponse response = paymentService.pay(userId, orderId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}