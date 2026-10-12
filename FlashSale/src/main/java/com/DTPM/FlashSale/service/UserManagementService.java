package com.DTPM.FlashSale.service;

import com.DTPM.FlashSale.dto.response.UserResponse;
import com.DTPM.FlashSale.domain.entity.User;
import com.DTPM.FlashSale.exception.AppException;
import com.DTPM.FlashSale.exception.ErrorCode;
import com.DTPM.FlashSale.mapper.UserMapper;
import com.DTPM.FlashSale.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public Page<UserResponse> listUsers(String keyword, Pageable pageable) {
        Page<User> page;
        if (keyword != null && !keyword.isBlank()) {
            page = userRepository.searchUsers(keyword.trim(), pageable);
        } else {
            page = userRepository.findByDeletedAtIsNull(pageable);
        }

        return page.map(userMapper::toUserResponse);
    }

    @Transactional
    public UserResponse banUser(Long adminId, Long targetUserId) {
        if (adminId.equals(targetUserId)) {
            throw new AppException(ErrorCode.CANNOT_BAN_SELF);
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        targetUser.ban();
        User updated = userRepository.save(targetUser);

        auditLogService.record(adminId, "BAN", "User", targetUserId,
                "Admin id=" + adminId + " banned user id=" + targetUserId);

        log.info("Admin id={} banned user id={}", adminId, targetUserId);
        return userMapper.toUserResponse(updated);
    }

    @Transactional
    public UserResponse unbanUser(Long adminId, Long targetUserId) {
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        targetUser.unban();
        User updated = userRepository.save(targetUser);

        auditLogService.record(adminId, "UNBAN", "User", targetUserId,
                "Admin id=" + adminId + " unbanned user id=" + targetUserId);

        log.info("Admin id={} unbanned user id={}", adminId, targetUserId);
        return userMapper.toUserResponse(updated);
    }
}
