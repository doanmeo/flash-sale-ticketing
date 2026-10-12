package com.DTPM.FlashSale.domain.repository;

import com.DTPM.FlashSale.domain.entity.Zone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ZoneRepository extends JpaRepository<Zone, Long> {
    List<Zone> findByVenueId(Long venueId);
}
