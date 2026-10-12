package com.DTPM.FlashSale.domain.entity;

import com.DTPM.FlashSale.domain.enums.CampaignStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "flashsale_campaigns")
public class FlashsaleCampaign extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "campaign_name", nullable = false, length = 150)
    private String campaignName;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CampaignStatus status = CampaignStatus.ACTIVE;

    @Column(name = "max_per_user")
    @Builder.Default
    private Integer maxPerUser = 2;

    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<FlashsaleItem> items = new ArrayList<>();

    public boolean isActiveNow() {
        LocalDateTime now = LocalDateTime.now();
        return status == CampaignStatus.ACTIVE
                && !now.isBefore(startAt)
                && !now.isAfter(endAt);
    }
}
