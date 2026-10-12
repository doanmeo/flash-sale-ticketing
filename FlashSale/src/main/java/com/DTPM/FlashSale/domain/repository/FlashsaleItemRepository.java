package com.DTPM.FlashSale.domain.repository;

import com.DTPM.FlashSale.domain.entity.FlashsaleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FlashsaleItemRepository extends JpaRepository<FlashsaleItem, Long> {

    List<FlashsaleItem> findByCampaignId(Long campaignId);

    @Query("SELECT fi FROM FlashsaleItem fi WHERE fi.ticketType.id = :ticketTypeId " +
            "AND fi.campaign.status = 'ACTIVE' " +
            "AND :now >= fi.campaign.startAt AND :now <= fi.campaign.endAt " +
            "AND fi.flashAvailable > 0")
    Optional<FlashsaleItem> findActiveFlashSaleItem(@Param("ticketTypeId") Long ticketTypeId, @Param("now") LocalDateTime now);
}
