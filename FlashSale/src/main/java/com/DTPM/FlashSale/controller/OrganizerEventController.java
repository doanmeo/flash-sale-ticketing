package com.DTPM.FlashSale.controller;

import com.DTPM.FlashSale.dto.request.CreateEventRequest;
import com.DTPM.FlashSale.dto.request.CreateTicketTypeRequest;
import com.DTPM.FlashSale.dto.request.UpdateEventRequest;
import com.DTPM.FlashSale.dto.request.UpdateTicketTypeRequest;
import com.DTPM.FlashSale.dto.response.ApiResponse;
import com.DTPM.FlashSale.dto.response.EventResponse;
import com.DTPM.FlashSale.dto.response.OrderResponse;
import com.DTPM.FlashSale.dto.response.OrganizerEventStatsResponse;
import com.DTPM.FlashSale.dto.response.TicketTypeResponse;
import com.DTPM.FlashSale.domain.enums.OrderStatus;
import com.DTPM.FlashSale.security.SecurityUtils;
import com.DTPM.FlashSale.service.EventService;
import com.DTPM.FlashSale.service.OrderService;
import com.DTPM.FlashSale.service.StatsService;
import com.DTPM.FlashSale.service.TicketTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;


@Tag(name = "Events")
@RestController
@RequestMapping("/api/organizer/events")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
public class OrganizerEventController {

    private final EventService eventService;
    private final TicketTypeService ticketTypeService;
    private final OrderService orderService;
    private final StatsService statsService;
    private final com.DTPM.FlashSale.service.ShowSessionService showSessionService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<EventResponse>>> getMyEvents(
            @PageableDefault(size = 10) Pageable pageable) {
        Long organizerId = SecurityUtils.getCurrentUserId();
        Page<EventResponse> events = eventService.getOrganizerEvents(organizerId, pageable);
        return ResponseEntity.ok(ApiResponse.success(events));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EventResponse>> createEvent(
            @Valid @RequestBody CreateEventRequest request) {
        Long organizerId = SecurityUtils.getCurrentUserId();
        EventResponse response = eventService.createEvent(organizerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EventResponse>> updateEvent(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEventRequest request) {
        Long organizerId = SecurityUtils.getCurrentUserId();
        EventResponse response = eventService.updateEvent(organizerId, id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<ApiResponse<EventResponse>> publishEvent(@PathVariable Long id) {
        Long organizerId = SecurityUtils.getCurrentUserId();
        EventResponse response = eventService.publishEvent(organizerId, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteEvent(@PathVariable Long id) {
        Long organizerId = SecurityUtils.getCurrentUserId();
        eventService.softDeleteEvent(organizerId, id);
        return ResponseEntity.ok(ApiResponse.success("Event deleted successfully"));
    }

    @PostMapping("/{eventId}/tickets")
    public ResponseEntity<ApiResponse<TicketTypeResponse>> createTicketType(
            @PathVariable Long eventId,
            @Valid @RequestBody CreateTicketTypeRequest request) {
        Long organizerId = SecurityUtils.getCurrentUserId();
        TicketTypeResponse response = ticketTypeService.createTicketType(organizerId, eventId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PutMapping("/tickets/{ticketTypeId}")
    public ResponseEntity<ApiResponse<TicketTypeResponse>> updateTicketType(
            @PathVariable Long ticketTypeId,
            @Valid @RequestBody UpdateTicketTypeRequest request) {
        Long organizerId = SecurityUtils.getCurrentUserId();
        TicketTypeResponse response = ticketTypeService.updateTicketType(organizerId, ticketTypeId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/tickets/{ticketTypeId}")
    public ResponseEntity<ApiResponse<String>> deleteTicketType(@PathVariable Long ticketTypeId) {
        Long organizerId = SecurityUtils.getCurrentUserId();
        ticketTypeService.softDeleteTicketType(organizerId, ticketTypeId);
        return ResponseEntity.ok(ApiResponse.success("Ticket type deleted successfully"));
    }

    @GetMapping("/{eventId}/orders")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getEventOrders(
            @PathVariable Long eventId,
            @RequestParam(required = false) OrderStatus status,
            @PageableDefault(size = 10) Pageable pageable) {
        Long organizerId = SecurityUtils.getCurrentUserId();
        Page<OrderResponse> orders = orderService.getEventOrders(organizerId, eventId, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/{eventId}/orders/export")
    public ResponseEntity<byte[]> exportEventOrdersToCsv(@PathVariable Long eventId) {
        Long organizerId = SecurityUtils.getCurrentUserId();
        byte[] csvData = orderService.exportEventOrdersToCsv(organizerId, eventId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("orders-" + eventId + ".csv")
                .build());

        return new ResponseEntity<>(csvData, headers, HttpStatus.OK);
    }

    @GetMapping("/{eventId}/stats")
    public ResponseEntity<ApiResponse<OrganizerEventStatsResponse>> getEventStats(@PathVariable Long eventId) {
        Long organizerId = SecurityUtils.getCurrentUserId();
        OrganizerEventStatsResponse stats = statsService.getOrganizerEventStats(organizerId, eventId);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @PostMapping("/{eventId}/sessions")
    public ResponseEntity<ApiResponse<com.DTPM.FlashSale.dto.response.ShowSessionResponse>> createSession(
            @PathVariable Long eventId,
            @Valid @RequestBody com.DTPM.FlashSale.dto.request.CreateShowSessionRequest request) {
        Long organizerId = SecurityUtils.getCurrentUserId();
        com.DTPM.FlashSale.dto.response.ShowSessionResponse response = showSessionService.createSession(organizerId, eventId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping("/{eventId}/sessions")
    public ResponseEntity<ApiResponse<java.util.List<com.DTPM.FlashSale.dto.response.ShowSessionResponse>>> getSessions(
            @PathVariable Long eventId) {
        java.util.List<com.DTPM.FlashSale.dto.response.ShowSessionResponse> response = showSessionService.getSessionsByEvent(eventId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

