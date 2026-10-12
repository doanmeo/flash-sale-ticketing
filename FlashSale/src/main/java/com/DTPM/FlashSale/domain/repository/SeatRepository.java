package com.DTPM.FlashSale.domain.repository;

import com.DTPM.FlashSale.domain.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findByZoneId(Long zoneId);
    List<Seat> findByZoneIdAndIsActiveTrue(Long zoneId);
}
