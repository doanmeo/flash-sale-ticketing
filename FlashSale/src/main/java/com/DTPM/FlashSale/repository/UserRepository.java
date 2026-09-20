package com.DTPM.FlashSale.repository;

import com.DTPM.FlashSale.entity.User;
import com.DTPM.FlashSale.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Page<User> findByDeletedAtIsNull(Pageable pageable);
    Page<User> findByFullNameContainingIgnoreCaseAndDeletedAtIsNull(String keyword, Pageable pageable);
    Page<User> findByStatusAndDeletedAtIsNull(UserStatus status, Pageable pageable);
}
