package com.DTPM.FlashSale.dto.response;

import com.DTPM.FlashSale.domain.enums.SessionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowSessionResponse {
    private Long id;
    private Long eventId;
    private String sessionName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private SessionStatus status;
    private List<TicketTypeResponse> ticketTypes;
}
