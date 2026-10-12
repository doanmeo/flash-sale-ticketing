package com.DTPM.FlashSale.domain.repository;

import com.DTPM.FlashSale.domain.entity.TicketType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TicketTypeRepository extends JpaRepository<TicketType, Long> {
    Optional<TicketType> findByIdAndDeletedAtIsNull(Long id);
    List<TicketType> findByEventIdAndDeletedAtIsNull(Long eventId);
    List<TicketType> findByEventIdAndIsHiddenFalseAndDeletedAtIsNull(Long eventId);
    List<TicketType> findByShowSessionIdAndDeletedAtIsNull(Long showSessionId);
    Optional<TicketType> findFirstByShowSessionIdAndZoneIdAndDeletedAtIsNull(Long showSessionId, Long zoneId);
    Optional<TicketType> findFirstByZoneIdAndDeletedAtIsNull(Long zoneId);
}