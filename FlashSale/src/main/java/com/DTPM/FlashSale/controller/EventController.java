package com.DTPM.FlashSale.controller;

import com.DTPM.FlashSale.dto.response.ApiResponse;
import com.DTPM.FlashSale.dto.response.EventResponse;
import com.DTPM.FlashSale.dto.response.TicketTypeResponse;
import com.DTPM.FlashSale.service.EventService;
import com.DTPM.FlashSale.service.TicketTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Events")
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;
    private final TicketTypeService ticketTypeService;
    private final com.DTPM.FlashSale.service.ShowSessionService showSessionService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<EventResponse>>> getPublishedEvents(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<EventResponse> events = eventService.getPublishedEvents(keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(events));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EventResponse>> getEventDetail(@PathVariable Long id) {
        EventResponse response = eventService.getEventDetail(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}/tickets")
    public ResponseEntity<ApiResponse<List<TicketTypeResponse>>> getEventTicketTypes(@PathVariable Long id) {
        List<TicketTypeResponse> response = ticketTypeService.getTicketTypesByEventId(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}/sessions")
    public ResponseEntity<ApiResponse<List<com.DTPM.FlashSale.dto.response.ShowSessionResponse>>> getEventSessions(@PathVariable Long id) {
        List<com.DTPM.FlashSale.dto.response.ShowSessionResponse> response = showSessionService.getSessionsByEvent(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
