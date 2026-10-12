package com.DTPM.FlashSale.domain.entity;

import com.DTPM.FlashSale.domain.enums.SeatStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "session_seats", uniqueConstraints = {
        @UniqueConstraint(name = "uk_session_seat", columnNames = {"session_id", "seat_id"})
}, indexes = {
        @Index(name = "idx_session_seat_status", columnList = "session_id, seat_status")
})
public class SessionSeat extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private ShowSession showSession;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @Enumerated(EnumType.STRING)
    @Column(name = "seat_status", nullable = false, length = 20)
    @Builder.Default
    private SeatStatus seatStatus = SeatStatus.AVAILABLE;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "locked_by_user_id")
    private Long lockedByUserId;

    public boolean isAvailable() {
        if (seatStatus == SeatStatus.BOOKED) {
            return false;
        }
        if (seatStatus == SeatStatus.LOCKED) {
            return lockedUntil != null && LocalDateTime.now().isAfter(lockedUntil);
        }
        return seatStatus == SeatStatus.AVAILABLE;
    }

    public boolean isLocked() {
        return seatStatus == SeatStatus.LOCKED && lockedUntil != null && LocalDateTime.now().isBefore(lockedUntil);
    }

    public boolean isBooked() {
        return seatStatus == SeatStatus.BOOKED;
    }

    public void lock(Long userId, LocalDateTime until) {
        this.seatStatus = SeatStatus.LOCKED;
        this.lockedByUserId = userId;
        this.lockedUntil = until;
    }

    public void unlock() {
        this.seatStatus = SeatStatus.AVAILABLE;
        this.lockedByUserId = null;
        this.lockedUntil = null;
    }

    public void book() {
        this.seatStatus = SeatStatus.BOOKED;
        this.lockedUntil = null;
    }
}
