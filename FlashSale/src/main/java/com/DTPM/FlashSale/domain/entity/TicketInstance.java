package com.DTPM.FlashSale.domain.entity;

import com.DTPM.FlashSale.domain.enums.TicketInstanceStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ticket_instances", indexes = {
        @Index(name = "uk_ticket_code", columnList = "ticket_code", unique = true),
        @Index(name = "idx_ticket_user", columnList = "user_id")
})
public class TicketInstance extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_code", nullable = false, unique = true, length = 50)
    private String ticketCode;

    @Column(name = "qr_payload", nullable = false, length = 500)
    private String qrPayload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TicketInstanceStatus status = TicketInstanceStatus.VALID;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "checked_in_at")
    private LocalDateTime checkedInAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public boolean isValid() {
        return status == TicketInstanceStatus.VALID;
    }

    public void checkIn() {
        this.status = TicketInstanceStatus.USED;
        this.checkedInAt = LocalDateTime.now();
    }

    public void cancel() {
        this.status = TicketInstanceStatus.CANCELLED;
    }
}
