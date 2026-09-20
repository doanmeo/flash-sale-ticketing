package com.DTPM.FlashSale.repository;

import com.DTPM.FlashSale.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
