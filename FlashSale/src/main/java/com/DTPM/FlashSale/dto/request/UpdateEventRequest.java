package com.DTPM.FlashSale.dto.request;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEventRequest {

    private String name;
    private String description;
    private String location;
    private LocalDateTime eventDate;
    private String imageUrl;
}
