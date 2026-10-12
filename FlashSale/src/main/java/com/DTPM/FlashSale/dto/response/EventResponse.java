package com.DTPM.FlashSale.dto.response;

import com.DTPM.FlashSale.domain.enums.EventStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventResponse {

    private Long id;
    private String name;
    private String description;
    private String location;
    private LocalDateTime eventDate;
    private String imageUrl;
    private EventStatus status;
    private Long organizerId;
    private String organizerName;
    private LocalDateTime createdAt;
    private List<TicketTypeResponse> ticketTypes;
}
