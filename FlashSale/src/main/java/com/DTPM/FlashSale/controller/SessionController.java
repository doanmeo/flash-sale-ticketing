package com.DTPM.FlashSale.controller;

import com.DTPM.FlashSale.dto.response.ApiResponse;
import com.DTPM.FlashSale.dto.response.SessionSeatResponse;
import com.DTPM.FlashSale.service.ShowSessionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Sessions")
@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final ShowSessionService showSessionService;

    @GetMapping("/{id}/seats")
    public ResponseEntity<ApiResponse<List<SessionSeatResponse>>> getSessionSeatMap(@PathVariable Long id) {
        List<SessionSeatResponse> seatMap = showSessionService.getSeatMap(id);
        return ResponseEntity.ok(ApiResponse.success(seatMap));
    }
}
