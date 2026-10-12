package com.DTPM.FlashSale.domain.repository;

import com.DTPM.FlashSale.domain.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Page<AuditLog> findByActorId(Long actorId, Pageable pageable);
}

