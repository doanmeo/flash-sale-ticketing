package com.DTPM.FlashSale.dto.response;

import com.DTPM.FlashSale.domain.enums.SeatStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionSeatResponse {
    private Long sessionSeatId;
    private Long seatId;
    private String rowLabel;
    private String seatNumber;
    private Long zoneId;
    private String zoneName;
    private Boolean isSeated;
    private SeatStatus status;
    private BigDecimal price;
}
