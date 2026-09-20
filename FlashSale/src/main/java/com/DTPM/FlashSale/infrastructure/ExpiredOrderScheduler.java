package com.DTPM.FlashSale.infrastructure;

import com.DTPM.FlashSale.entity.Order;
import com.DTPM.FlashSale.enums.OrderStatus;
import com.DTPM.FlashSale.repository.OrderRepository;
import com.DTPM.FlashSale.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * UC-09 nhánh 2: Auto-expire đơn PENDING quá expiresAt
 * Chạy mỗi 60 giây
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExpiredOrderScheduler {

    private final OrderRepository orderRepository;
    private final OrderService orderService;

    @Scheduled(fixedDelayString = "${app.order.expire-scan-ms:60000}")
    public void processExpiredOrders() {
        LocalDateTime now = LocalDateTime.now();
        List<Order> expired = orderRepository.findByStatusAndExpiresAtBefore(
                OrderStatus.PENDING,
                now
        );

        if (expired.isEmpty()) {
            return;
        }

        log.info("Found {} expired PENDING orders", expired.size());
        for (Order order : expired) {
            try {
                orderService.expire(order.getId());
            } catch (Exception e) {
                log.error("Failed to expire order id={}", order.getId(), e);
            }
        }
    }
}