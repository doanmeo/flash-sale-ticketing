package com.DTPM.FlashSale.service;

import com.DTPM.FlashSale.dto.response.UserResponse;
import com.DTPM.FlashSale.entity.User;
import com.DTPM.FlashSale.enums.UserRole;
import com.DTPM.FlashSale.enums.UserStatus;
import com.DTPM.FlashSale.exception.AppException;
import com.DTPM.FlashSale.exception.ErrorCode;
import com.DTPM.FlashSale.mapper.UserMapper;
import com.DTPM.FlashSale.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserManagementServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private UserManagementService userManagementService;

    private User admin;
    private User normalUser;

    @BeforeEach
    void setUp() {
        admin = User.builder().id(1L).email("admin@example.com").role(UserRole.ADMIN).status(UserStatus.ACTIVE).build();
        normalUser = User.builder().id(2L).email("user@example.com").role(UserRole.USER).status(UserStatus.ACTIVE).build();
    }

    @Test
    @DisplayName("banUser: Thất bại khi Admin tự cấm chính mình (CANNOT_BAN_SELF)")
    void banUser_CannotBanSelf() {
        AppException exception = assertThrows(AppException.class, () -> userManagementService.banUser(1L, 1L));
        assertEquals(ErrorCode.CANNOT_BAN_SELF, exception.getErrorCode());
    }

    @Test
    @DisplayName("banUser: Thất bại khi không tìm thấy target user (USER_NOT_FOUND)")
    void banUser_UserNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> userManagementService.banUser(1L, 999L));
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("banUser: Thành công cấm người dùng và ghi AuditLog")
    void banUser_Success() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(normalUser));
        when(userRepository.save(any(User.class))).thenReturn(normalUser);
        when(userMapper.toUserResponse(normalUser)).thenReturn(UserResponse.builder().id(2L).status(UserStatus.BANNED).build());

        UserResponse response = userManagementService.banUser(1L, 2L);

        assertNotNull(response);
        assertEquals(UserStatus.BANNED, normalUser.getStatus());
        verify(auditLogService, times(1)).record(eq(1L), eq("BAN"), eq("User"), eq(2L), anyString());
        verify(userRepository, times(1)).save(normalUser);
    }

    @Test
    @DisplayName("unbanUser: Thành công bỏ cấm người dùng và ghi AuditLog")
    void unbanUser_Success() {
        normalUser.setStatus(UserStatus.BANNED);
        when(userRepository.findById(2L)).thenReturn(Optional.of(normalUser));
        when(userRepository.save(any(User.class))).thenReturn(normalUser);
        when(userMapper.toUserResponse(normalUser)).thenReturn(UserResponse.builder().id(2L).status(UserStatus.ACTIVE).build());

        UserResponse response = userManagementService.unbanUser(1L, 2L);

        assertNotNull(response);
        assertEquals(UserStatus.ACTIVE, normalUser.getStatus());
        verify(auditLogService, times(1)).record(eq(1L), eq("UNBAN"), eq("User"), eq(2L), anyString());
        verify(userRepository, times(1)).save(normalUser);
    }
}
