package com.DTPM.FlashSale.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketTypeResponse {

    private Long id;
    private Long eventId;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer totalQuantity;
    private Integer availableQuantity;
    private Integer realtimeAvailable; // Real-time value from Redis
    private LocalDateTime saleStart;
    private LocalDateTime saleEnd;
    private Integer minPerOrder;
    private Integer maxPerOrder;
    private Boolean isHidden;
    private Boolean isOnSale;
}
