package com.DTPM.FlashSale.dto.response;

import com.DTPM.FlashSale.enums.OrderStatus;
import com.DTPM.FlashSale.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private Long id;
    private Long orderId;
    private String orderCode;
    private BigDecimal amount;
    private PaymentStatus paymentStatus;
    private OrderStatus orderStatus;
    private LocalDateTime paidAt;
    private String qrCodeUrl;
}
