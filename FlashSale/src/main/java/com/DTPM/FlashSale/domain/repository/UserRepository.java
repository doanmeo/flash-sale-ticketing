package com.DTPM.FlashSale.domain.repository;

import com.DTPM.FlashSale.domain.entity.User;
import com.DTPM.FlashSale.domain.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Page<User> findByDeletedAtIsNull(Pageable pageable);
    Page<User> findByFullNameContainingIgnoreCaseAndDeletedAtIsNull(String keyword, Pageable pageable);
    Page<User> findByStatusAndDeletedAtIsNull(UserStatus status, Pageable pageable);

    @Query("SELECT u FROM User u WHERE (LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND u.deletedAt IS NULL")
    Page<User> searchUsers(@Param("keyword") String keyword, Pageable pageable);
}
