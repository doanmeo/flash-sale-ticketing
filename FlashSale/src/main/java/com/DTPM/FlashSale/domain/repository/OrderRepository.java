package com.DTPM.FlashSale.domain.repository;

import com.DTPM.FlashSale.domain.entity.Order;
import com.DTPM.FlashSale.domain.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByIdAndUserId(Long id, Long userId);
    Optional<Order> findByOrderCode(String orderCode);
    Page<Order> findByUserId(Long userId, Pageable pageable);
    Page<Order> findByUserIdAndStatus(Long userId, OrderStatus status, Pageable pageable);
    List<Order> findByStatusAndExpiresAtBefore(OrderStatus status, LocalDateTime time);

    Page<Order> findByEventId(Long eventId, Pageable pageable);
    Page<Order> findByEventIdAndStatus(Long eventId, OrderStatus status, Pageable pageable);
    List<Order> findByEventId(Long eventId);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = com.DTPM.FlashSale.domain.enums.OrderStatus.PAID")
    long countPaidOrders();

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status = com.DTPM.FlashSale.domain.enums.OrderStatus.PAID")
    BigDecimal sumTotalPaidRevenue();

    @Query("SELECT COUNT(o) FROM Order o WHERE o.event.id = :eventId AND o.status = com.DTPM.FlashSale.domain.enums.OrderStatus.PAID")
    long countPaidOrdersByEventId(@Param("eventId") Long eventId);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.event.id = :eventId AND o.status = com.DTPM.FlashSale.domain.enums.OrderStatus.PAID")
    BigDecimal sumTotalPaidRevenueByEventId(@Param("eventId") Long eventId);
}
