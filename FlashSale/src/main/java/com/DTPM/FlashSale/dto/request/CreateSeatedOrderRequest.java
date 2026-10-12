package com.DTPM.FlashSale.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSeatedOrderRequest {

    @NotNull(message = "Session ID is required")
    private Long sessionId;

    @NotEmpty(message = "Seat IDs cannot be empty")
    private List<Long> seatIds;
}
