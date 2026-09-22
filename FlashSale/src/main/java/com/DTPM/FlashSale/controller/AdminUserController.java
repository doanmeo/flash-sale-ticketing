package com.DTPM.FlashSale.controller;

import com.DTPM.FlashSale.dto.response.ApiResponse;
import com.DTPM.FlashSale.dto.response.UserResponse;
import com.DTPM.FlashSale.security.SecurityUtils;
import com.DTPM.FlashSale.service.UserManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserManagementService userManagementService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<UserResponse>>> listUsers(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<UserResponse> users = userManagementService.listUsers(keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @PatchMapping("/{id}/ban")
    public ResponseEntity<ApiResponse<UserResponse>> banUser(@PathVariable Long id) {
        Long adminId = SecurityUtils.getCurrentUserId();
        UserResponse response = userManagementService.banUser(adminId, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{id}/unban")
    public ResponseEntity<ApiResponse<UserResponse>> unbanUser(@PathVariable Long id) {
        Long adminId = SecurityUtils.getCurrentUserId();
        UserResponse response = userManagementService.unbanUser(adminId, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
