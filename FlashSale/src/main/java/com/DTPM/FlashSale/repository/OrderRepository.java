package com.DTPM.FlashSale.repository;
import com.DTPM.FlashSale.entity.Order;
import com.DTPM.FlashSale.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByIdAndUserId(Long id, Long userId);
    Optional<Order> findByOrderCode(String orderCode);
    Page<Order> findByUserId(Long userId, Pageable pageable);
    Page<Order> findByUserIdAndStatus(Long userId, OrderStatus status, Pageable pageable);
    List<Order> findByStatusAndExpiresAtBefore(OrderStatus status, LocalDateTime time);

}
