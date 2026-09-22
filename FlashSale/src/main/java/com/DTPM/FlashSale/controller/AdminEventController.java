package com.DTPM.FlashSale.controller;

import com.DTPM.FlashSale.dto.request.UpdateEventRequest;
import com.DTPM.FlashSale.dto.response.ApiResponse;
import com.DTPM.FlashSale.dto.response.EventResponse;
import com.DTPM.FlashSale.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/events")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminEventController {

    private final EventService eventService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<EventResponse>>> listEvents(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<EventResponse> events = eventService.getAdminEvents(keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(events));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EventResponse>> updateEvent(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEventRequest request) {
        EventResponse response = eventService.adminUpdateEvent(id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteEvent(@PathVariable Long id) {
        eventService.adminDeleteEvent(id);
        return ResponseEntity.ok(ApiResponse.success("Event deleted by admin successfully"));
    }
}
