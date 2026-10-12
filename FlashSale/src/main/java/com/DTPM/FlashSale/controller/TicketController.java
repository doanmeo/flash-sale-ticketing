package com.DTPM.FlashSale.controller;

import com.DTPM.FlashSale.dto.response.ApiResponse;
import com.DTPM.FlashSale.dto.response.TicketInstanceResponse;
import com.DTPM.FlashSale.security.SecurityUtils;
import com.DTPM.FlashSale.service.TicketInstanceService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Tickets")
@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketInstanceService ticketInstanceService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<TicketInstanceResponse>>> getMyTickets() {
        Long userId = SecurityUtils.getCurrentUserId();
        List<TicketInstanceResponse> tickets = ticketInstanceService.getMyTickets(userId);
        return ResponseEntity.ok(ApiResponse.success(tickets));
    }
}
