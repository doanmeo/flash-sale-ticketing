package com.DTPM.FlashSale.controller;

import com.DTPM.FlashSale.dto.response.AdminStatsResponse;
import com.DTPM.FlashSale.dto.response.ApiResponse;
import com.DTPM.FlashSale.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminStatsController {

    private final StatsService statsService;

    @GetMapping
    public ResponseEntity<ApiResponse<AdminStatsResponse>> getAdminStats() {
        AdminStatsResponse stats = statsService.getAdminStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
