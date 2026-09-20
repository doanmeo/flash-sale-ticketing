package com.DTPM.FlashSale.repository;

import com.DTPM.FlashSale.entity.TicketType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TicketTypeRepository extends JpaRepository<TicketType, Long> {
    Optional<TicketType> findByIdAndDeletedAtIsNull(Long id);
    List<TicketType> findByEventIdAndDeletedAtIsNull(Long eventId);
    List<TicketType> findByEventIdAndIsHiddenFalseAndDeletedAtIsNull(Long eventId);
}