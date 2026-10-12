package com.DTPM.FlashSale.domain.repository;

import com.DTPM.FlashSale.domain.entity.FlashsaleCampaign;
import com.DTPM.FlashSale.domain.enums.CampaignStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FlashsaleCampaignRepository extends JpaRepository<FlashsaleCampaign, Long> {
    List<FlashsaleCampaign> findByStatus(CampaignStatus status);
}
