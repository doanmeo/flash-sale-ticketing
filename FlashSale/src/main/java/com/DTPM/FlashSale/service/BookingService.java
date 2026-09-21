package com.DTPM.FlashSale.service;

import com.DTPM.FlashSale.dto.request.CreateOrderRequest;
import com.DTPM.FlashSale.dto.response.OrderResponse;
import com.DTPM.FlashSale.entity.*;
import com.DTPM.FlashSale.enums.OrderStatus;
import com.DTPM.FlashSale.exception.AppException;
import com.DTPM.FlashSale.exception.ErrorCode;
import com.DTPM.FlashSale.infrastructure.RedisStockService;
import com.DTPM.FlashSale.mapper.OrderMapper;
import com.DTPM.FlashSale.repository.OrderRepository;
import com.DTPM.FlashSale.repository.TicketTypeRepository;
import com.DTPM.FlashSale.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingService {

    private static final int HOLD_MINUTES = 10;

    private final TicketTypeRepository ticketTypeRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final RedisStockService redisStockService;
    private final OrderMapper orderMapper;

    /**
     * UC-06: Đặt vé Flash Sale
     * validate → atomicDecrease Redis → Order PENDING + hold 10p + OrderItem
     * snapshot
     */
    @Transactional
    public OrderResponse createOrder(Long userId, CreateOrderRequest request) {
        // 1. User
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        if (!user.canLogin()) {
            throw new AppException(ErrorCode.USER_BANNED);
        }

        // 2. TicketType
        TicketType ticketType = ticketTypeRepository.findByIdAndDeletedAtIsNull(request.getTicketTypeId())
                .orElseThrow(() -> new AppException(ErrorCode.TICKET_TYPE_NOT_FOUND));
        Event event = ticketType.getEvent();
        if (event == null || !event.isPublished()) {
            throw new AppException(ErrorCode.EVENT_NOT_PUBLISHED);
        }

        // 3. Validate nghiệp vụ
        validateBooking(ticketType, request.getQuantity());

        // 4. Atomic DECR Redis – chống overselling
        boolean reserved = redisStockService.atomicDecrease(
                ticketType.getId(),
                request.getQuantity());
        if (!reserved) {
            throw new AppException(ErrorCode.SOLD_OUT);
        }

        try {
            // 5. Tạo Order PENDING + hold 10 phút
            LocalDateTime now = LocalDateTime.now();
            BigDecimal unitPrice = ticketType.getPrice();
            BigDecimal total = unitPrice.multiply(BigDecimal.valueOf(request.getQuantity()));

            Order order = Order.builder()
                    .orderCode(generateOrderCode())
                    .user(user)
                    .event(event)
                    .totalAmount(total)
                    .status(OrderStatus.PENDING)
                    .expiresAt(now.plusMinutes(HOLD_MINUTES))
                    .build();

            // 6. OrderItem – snapshot unitPrice
            OrderItem item = OrderItem.builder()
                    .ticketType(ticketType)
                    .quantity(request.getQuantity())
                    .unitPrice(unitPrice)
                    .build();
            order.addItem(item);

            // Đồng bộ availableQuantity trên DB (đối soát; Redis là hot path)
            int current = ticketType.getAvailableQuantity() == null ? 0 : ticketType.getAvailableQuantity();
            ticketType.setAvailableQuantity(Math.max(0, current - request.getQuantity()));

            Order saved = orderRepository.save(order);
            return orderMapper.toOrderResponse(saved);
        } catch (RuntimeException ex) {
            // Nếu lỗi sau khi DECR → trả lại Redis
            redisStockService.increase(ticketType.getId(), request.getQuantity());
            throw ex;
        }
    }

    private void validateBooking(TicketType ticketType, int quantity) {
        if (!ticketType.isOnSale()) {
            throw new AppException(ErrorCode.TICKET_NOT_ON_SALE);
        }
        if (!ticketType.isQuantityValid(quantity)) {
            throw new AppException(ErrorCode.INVALID_TICKET_QUANTITY);
        }
    }

    private String generateOrderCode() {
        String date = LocalDateTime.now().toLocalDate().toString().replace("-", "");
        String rand = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "FS-" + date + "-" + rand;
    }
}