package com.DTPM.FlashSale.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "flashsale_items")
public class FlashsaleItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private FlashsaleCampaign campaign;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_type_id", nullable = false)
    private TicketType ticketType;

    @Column(name = "flash_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal flashPrice;

    @Column(name = "flash_quota", nullable = false)
    private Integer flashQuota;

    @Column(name = "flash_available", nullable = false)
    private Integer flashAvailable;

    @Column(name = "per_user_limit")
    @Builder.Default
    private Integer perUserLimit = 2;

    public boolean hasAvailable(int quantity) {
        return flashAvailable != null && flashAvailable >= quantity;
    }

    public void decreaseAvailable(int quantity) {
        this.flashAvailable = Math.max(0, this.flashAvailable - quantity);
    }

    public void increaseAvailable(int quantity) {
        this.flashAvailable = Math.min(this.flashQuota, this.flashAvailable + quantity);
    }
}
