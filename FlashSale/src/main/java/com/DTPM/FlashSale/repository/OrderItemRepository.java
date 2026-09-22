package com.DTPM.FlashSale.repository;

import com.DTPM.FlashSale.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query("SELECT COALESCE(SUM(i.quantity), 0) FROM OrderItem i WHERE i.ticketType.id = :ticketTypeId AND i.order.status = com.DTPM.FlashSale.enums.OrderStatus.PAID")
    int sumSoldQuantityByTicketTypeId(@Param("ticketTypeId") Long ticketTypeId);
}
