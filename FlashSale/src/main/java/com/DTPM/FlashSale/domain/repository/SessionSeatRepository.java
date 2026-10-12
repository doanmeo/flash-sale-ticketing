package com.DTPM.FlashSale.domain.repository;

import com.DTPM.FlashSale.domain.entity.SessionSeat;
import com.DTPM.FlashSale.domain.enums.SeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SessionSeatRepository extends JpaRepository<SessionSeat, Long> {

    List<SessionSeat> findByShowSessionId(Long sessionId);

    Optional<SessionSeat> findByShowSessionIdAndSeatId(Long sessionId, Long seatId);

    List<SessionSeat> findByShowSessionIdAndSeatIdIn(Long sessionId, List<Long> seatIds);

    List<SessionSeat> findByShowSessionIdAndSeatStatus(Long sessionId, SeatStatus status);

    @Query("SELECT s FROM SessionSeat s WHERE s.seatStatus = 'LOCKED' AND s.lockedUntil IS NOT NULL AND s.lockedUntil < :now")
    List<SessionSeat> findExpiredLocks(@Param("now") LocalDateTime now);
}
