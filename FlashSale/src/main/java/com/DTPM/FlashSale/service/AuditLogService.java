package com.DTPM.FlashSale.service;

import com.DTPM.FlashSale.entity.AuditLog;
import com.DTPM.FlashSale.entity.User;
import com.DTPM.FlashSale.repository.AuditLogRepository;
import com.DTPM.FlashSale.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Transactional
    public void record(Long actorId, String action, String targetType, Long targetId, String detail) {
        User actor = actorId != null ? userRepository.findById(actorId).orElse(null) : null;

        AuditLog auditLog = AuditLog.builder()
                .actor(actor)
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .detail(detail)
                .build();

        auditLogRepository.save(auditLog);
        log.info("Recorded AuditLog: action={}, targetType={}, targetId={}, actorId={}",
                action, targetType, targetId, actorId);
    }
}
