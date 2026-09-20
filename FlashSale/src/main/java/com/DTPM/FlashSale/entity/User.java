package com.DTPM.FlashSale.entity;

import com.DTPM.FlashSale.enums.UserRole;
import com.DTPM.FlashSale.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public boolean isActive() {
        return status == UserStatus.ACTIVE && deletedAt == null;
    }

    public boolean isBanned() {
        return status == UserStatus.BANNED;
    }

    public boolean canLogin() {
        return isActive() && !isBanned();
    }

    public boolean isOrganizer() {
        return role == UserRole.ORGANIZER;
    }

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    public void ban() {
        this.status = UserStatus.BANNED;
    }

    public void unban() {
        this.status = UserStatus.ACTIVE;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }
}