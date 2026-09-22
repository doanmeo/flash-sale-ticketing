package com.DTPM.FlashSale.dto.request;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTicketTypeRequest {

    private String name;
    private String description;
    private BigDecimal price;
    private Integer totalQuantity;
    private LocalDateTime saleStart;
    private LocalDateTime saleEnd;
    private Integer minPerOrder;
    private Integer maxPerOrder;
    private Boolean isHidden;
}
