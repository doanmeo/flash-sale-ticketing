package com.DTPM.FlashSale.dto.response;

import com.DTPM.FlashSale.domain.enums.LineItemType;
import com.DTPM.FlashSale.domain.enums.TicketInstanceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketInstanceResponse {
    private Long id;
    private String ticketCode;
    private String qrPayload;
    private TicketInstanceStatus status;
    private LineItemType lineType;
    private String eventName;
    private String sessionName;
    private String zoneName;
    private String seatInfo; // e.g. "Row A - Seat 05" (if seated)
    private BigDecimal price;
    private LocalDateTime issuedAt;
}
