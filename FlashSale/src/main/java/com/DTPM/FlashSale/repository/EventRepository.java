package com.DTPM.FlashSale.repository;

import com.DTPM.FlashSale.entity.Event;
import com.DTPM.FlashSale.enums.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {
    Optional<Event> findByIdAndDeletedAtIsNull(Long id);
    Page<Event> findByStatusAndDeletedAtIsNull(EventStatus status, Pageable pageable);
    Page<Event> findByOrganizerIdAndDeletedAtIsNull(Long organizerId, Pageable pageable);
    Page<Event> findByNameContainingIgnoreCaseAndStatusAndDeletedAtIsNull(
            String keyword, EventStatus status, Pageable pageable);
}
