package com.DTPM.FlashSale.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatsResponse {

    private Long totalUsers;
    private Long totalEvents;
    private Long totalPaidOrders;
    private BigDecimal totalRevenue;
}
