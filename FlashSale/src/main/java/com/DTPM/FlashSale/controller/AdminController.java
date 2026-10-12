package com.DTPM.FlashSale.controller;

import com.DTPM.FlashSale.dto.request.UpdateEventRequest;
import com.DTPM.FlashSale.dto.response.AdminStatsResponse;
import com.DTPM.FlashSale.dto.response.ApiResponse;
import com.DTPM.FlashSale.dto.response.EventResponse;
import com.DTPM.FlashSale.dto.response.UserResponse;
import com.DTPM.FlashSale.security.SecurityUtils;
import com.DTPM.FlashSale.service.EventService;
import com.DTPM.FlashSale.service.StatsService;
import com.DTPM.FlashSale.service.UserManagementService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserManagementService userManagementService;
    private final StatsService statsService;
    private final EventService eventService;

    // --- User Management ---

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> listUsers(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<UserResponse> users = userManagementService.listUsers(keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @PatchMapping("/users/{id}/ban")
    public ResponseEntity<ApiResponse<UserResponse>> banUser(@PathVariable Long id) {
        Long adminId = SecurityUtils.getCurrentUserId();
        UserResponse response = userManagementService.banUser(adminId, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/users/{id}/unban")
    public ResponseEntity<ApiResponse<UserResponse>> unbanUser(@PathVariable Long id) {
        Long adminId = SecurityUtils.getCurrentUserId();
        UserResponse response = userManagementService.unbanUser(adminId, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // --- System Statistics ---

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AdminStatsResponse>> getAdminStats() {
        AdminStatsResponse stats = statsService.getAdminStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    // --- Event Moderation ---

    @GetMapping("/events")
    public ResponseEntity<ApiResponse<Page<EventResponse>>> listEvents(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<EventResponse> events = eventService.getAdminEvents(keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(events));
    }

    @PutMapping("/events/{id}")
    public ResponseEntity<ApiResponse<EventResponse>> updateEvent(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEventRequest request) {
        EventResponse response = eventService.adminUpdateEvent(id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/events/{id}")
    public ResponseEntity<ApiResponse<String>> deleteEvent(@PathVariable Long id) {
        eventService.adminDeleteEvent(id);
        return ResponseEntity.ok(ApiResponse.success("Event deleted by admin successfully"));
    }
}
