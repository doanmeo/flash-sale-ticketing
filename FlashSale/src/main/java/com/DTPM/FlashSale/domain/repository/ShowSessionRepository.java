package com.DTPM.FlashSale.domain.repository;

import com.DTPM.FlashSale.domain.entity.ShowSession;
import com.DTPM.FlashSale.domain.enums.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShowSessionRepository extends JpaRepository<ShowSession, Long> {
    List<ShowSession> findByEventId(Long eventId);
    List<ShowSession> findByEventIdAndStatus(Long eventId, SessionStatus status);
}
