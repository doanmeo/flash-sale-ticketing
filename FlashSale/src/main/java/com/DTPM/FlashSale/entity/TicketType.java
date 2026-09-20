package com.DTPM.FlashSale.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ticket_types")
public class TicketType extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "total_quantity", nullable = false)
    private Integer totalQuantity;

    @Column(name = "available_quantity", nullable = false)
    private Integer availableQuantity;

    @Column(name = "sale_start", nullable = false)
    private LocalDateTime saleStart;

    @Column(name = "sale_end", nullable = false)
    private LocalDateTime saleEnd;

    @Column(name = "min_per_order", nullable = false)
    @Builder.Default
    private Integer minPerOrder = 1;

    @Column(name = "max_per_order", nullable = false)
    @Builder.Default
    private Integer maxPerOrder = 10;

    @Column(name = "is_hidden", nullable = false)
    @Builder.Default
    private Boolean isHidden = false;

    @Column(name = "allow_transfer")
    @Builder.Default
    private Boolean allowTransfer = false;

    @Column(name = "allow_refund")
    @Builder.Default
    private Boolean allowRefund = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    public boolean isOnSale() {
        LocalDateTime now = LocalDateTime.now();
        return deletedAt == null
                && Boolean.FALSE.equals(isHidden)
                && !now.isBefore(saleStart)
                && !now.isAfter(saleEnd);
    }

    public boolean isQuantityValid(int quantity) {
        return quantity >= minPerOrder && quantity <= maxPerOrder;
    }

    public boolean hasStock() {
        return availableQuantity != null && availableQuantity > 0;
    }

    public void hide() {
        this.isHidden = true;
    }

    public void show() {
        this.isHidden = false;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }
}