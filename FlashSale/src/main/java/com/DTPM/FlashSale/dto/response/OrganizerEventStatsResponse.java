package com.DTPM.FlashSale.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizerEventStatsResponse {

    private Long eventId;
    private String eventName;
    private Long totalPaidOrders;
    private BigDecimal totalRevenue;
    private List<TicketTypeStat> ticketTypeStats;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TicketTypeStat {
        private Long ticketTypeId;
        private String ticketTypeName;
        private BigDecimal price;
        private Integer totalQuantity;
        private Integer soldQuantity;
        private Integer remainingQuantity;
    }
}
