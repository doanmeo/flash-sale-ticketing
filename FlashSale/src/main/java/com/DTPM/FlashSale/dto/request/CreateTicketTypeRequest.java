package com.DTPM.FlashSale.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTicketTypeRequest {

    @NotBlank(message = "Ticket name is required")
    private String name;

    private String description;

    @NotNull(message = "Price is required")
    @Min(value = 0, message = "Price must be >= 0")
    private BigDecimal price;

    @NotNull(message = "Total quantity is required")
    @Min(value = 1, message = "Total quantity must be > 0")
    private Integer totalQuantity;

    @NotNull(message = "Sale start time is required")
    private LocalDateTime saleStart;

    @NotNull(message = "Sale end time is required")
    private LocalDateTime saleEnd;

    @Builder.Default
    private Integer minPerOrder = 1;

    @Builder.Default
    private Integer maxPerOrder = 10;

    @Builder.Default
    private Boolean isHidden = false;
}
