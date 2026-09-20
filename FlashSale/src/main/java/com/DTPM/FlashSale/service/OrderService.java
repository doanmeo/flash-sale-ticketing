package com.DTPM.FlashSale.service;

import com.DTPM.FlashSale.dto.response.OrderResponse;
import com.DTPM.FlashSale.entity.Order;
import com.DTPM.FlashSale.entity.OrderItem;
import com.DTPM.FlashSale.entity.TicketType;
import com.DTPM.FlashSale.enums.OrderStatus;
import com.DTPM.FlashSale.exception.AppException;
import com.DTPM.FlashSale.exception.ErrorCode;
import com.DTPM.FlashSale.infrastructure.RedisStockService;
import com.DTPM.FlashSale.repository.OrderRepository;
import com.DTPM.FlashSale.repository.TicketTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final RedisStockService redisStockService;

    /**
     * UC-09 nhánh 1: User hủy đơn PENDING (còn trong thời gian hold)
     */
    @Transactional
    public OrderResponse cancel(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getUser().getId().equals(userId)) {
            throw new AppException(ErrorCode.ORDER_ACCESS_DENIED);
        }

        if (!order.canBeCancelled()) {
            if (order.isExpired() || order.getStatus() == OrderStatus.EXPIRED) {
                throw new AppException(ErrorCode.ORDER_EXPIRED);
            }
            throw new AppException(ErrorCode.ORDER_CANNOT_CANCEL);
        }

        order.cancel(); // status = CANCELLED
        releaseStock(order);
        Order saved = orderRepository.save(order);

        log.info("Order {} cancelled by user {}", order.getOrderCode(), userId);
        return toResponse(saved);
    }

    /**
     * Hệ thống hết hạn 1 đơn (idempotent: chỉ xử lý khi còn PENDING)
     */
    @Transactional
    public void expire(Long orderId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            return;
        }

        // Idempotent: nếu không còn PENDING thì return
        if (order.getStatus() != OrderStatus.PENDING) {
            return;
        }

        order.expire(); // status = EXPIRED
        releaseStock(order);
        orderRepository.save(order);
        log.info("Order {} expired by system", order.getOrderCode());
    }

    /**
     * Trả lại inventory Redis (+ đồng bộ DB availableQuantity)
     */
    private void releaseStock(Order order) {
        for (OrderItem item : order.getItems()) {
            TicketType ticketType = item.getTicketType();
            Long ticketTypeId = ticketType.getId();
            int qty = item.getQuantity();

            redisStockService.increase(ticketTypeId, qty);

            // Đồng bộ DB (đối soát)
            int current = ticketType.getAvailableQuantity() == null ? 0 : ticketType.getAvailableQuantity();
            ticketType.setAvailableQuantity(current + qty);
            ticketTypeRepository.save(ticketType);
        }
    }

    private OrderResponse toResponse(Order order) {
        List<OrderResponse.OrderItemResponse> items = order.getItems().stream()
                .map(i -> OrderResponse.OrderItemResponse.builder()
                        .ticketTypeId(i.getTicketType().getId())
                        .ticketTypeName(i.getTicketType().getName())
                        .quantity(i.getQuantity())
                        .unitPrice(i.getUnitPrice())
                        .subtotal(i.calculateSubtotal())
                        .build())
                .toList();

        return OrderResponse.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .expiresAt(order.getExpiresAt())
                .createdAt(order.getCreatedAt())
                .items(items)
                .build();
    }
}