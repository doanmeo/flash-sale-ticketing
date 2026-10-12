package com.DTPM.FlashSale.infrastructure;

import com.DTPM.FlashSale.domain.entity.Order;
import com.DTPM.FlashSale.domain.enums.OrderStatus;
import com.DTPM.FlashSale.domain.repository.OrderRepository;
import com.DTPM.FlashSale.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * UC-09 nhánh 2: Auto-expire đơn PENDING quá expiresAt
 * Chạy mỗi 60 giây (hoặc app.order.expire-scan-ms)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExpiredOrderScheduler {

    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final com.DTPM.FlashSale.domain.repository.SessionSeatRepository sessionSeatRepository;
    private final RedisSeatLockService redisSeatLockService;

    @Scheduled(fixedDelayString = "${app.order.expire-scan-ms:60000}")
    public void processExpiredOrders() {
        LocalDateTime now = LocalDateTime.now();
        List<Order> expired = orderRepository.findByStatusAndExpiresAtBefore(
                OrderStatus.PENDING,
                now
        );

        if (!expired.isEmpty()) {
            log.info("Found {} expired PENDING orders", expired.size());
            for (Order order : expired) {
                try {
                    orderService.expire(order.getId());
                } catch (Exception e) {
                    log.error("Failed to expire order id={}", order.getId(), e);
                }
            }
        }

        // Dọn dẹp an toàn các ghế bị lock mồ côi quá hạn
        try {
            List<com.DTPM.FlashSale.domain.entity.SessionSeat> orphanLocks = sessionSeatRepository.findExpiredLocks(now);
            for (com.DTPM.FlashSale.domain.entity.SessionSeat ss : orphanLocks) {
                ss.unlock();
                sessionSeatRepository.save(ss);
                if (ss.getShowSession() != null && ss.getSeat() != null) {
                    redisSeatLockService.releaseSeatLock(ss.getShowSession().getId(), ss.getSeat().getId());
                }
            }
        } catch (Exception e) {
            log.error("Failed to clean up orphan seat locks", e);
        }
    }
}