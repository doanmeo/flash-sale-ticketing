package com.DTPM.FlashSale.domain.repository;

import com.DTPM.FlashSale.domain.entity.TicketInstance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TicketInstanceRepository extends JpaRepository<TicketInstance, Long> {

    List<TicketInstance> findByUserIdOrderByIssuedAtDesc(Long userId);

    Optional<TicketInstance> findByTicketCode(String ticketCode);

    List<TicketInstance> findByOrderItemOrderId(Long orderId);
}
