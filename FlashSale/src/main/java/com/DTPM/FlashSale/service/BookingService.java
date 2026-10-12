package com.DTPM.FlashSale.service;

import com.DTPM.FlashSale.dto.request.CreateOrderRequest;
import com.DTPM.FlashSale.dto.request.CreateSeatedOrderRequest;
import com.DTPM.FlashSale.dto.response.OrderResponse;
import com.DTPM.FlashSale.domain.entity.*;
import com.DTPM.FlashSale.domain.enums.LineItemType;
import com.DTPM.FlashSale.domain.enums.OrderStatus;
import com.DTPM.FlashSale.domain.enums.SeatStatus;
import com.DTPM.FlashSale.exception.AppException;
import com.DTPM.FlashSale.exception.ErrorCode;
import com.DTPM.FlashSale.infrastructure.RedisSeatLockService;
import com.DTPM.FlashSale.infrastructure.RedisStockService;
import com.DTPM.FlashSale.mapper.OrderMapper;
import com.DTPM.FlashSale.domain.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    @Value("${app.order.hold-minutes:10}")
    private int holdMinutes;

    private final TicketTypeRepository ticketTypeRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final RedisStockService redisStockService;
    private final OrderMapper orderMapper;
    private final ShowSessionRepository showSessionRepository;
    private final SessionSeatRepository sessionSeatRepository;
    private final RedisSeatLockService redisSeatLockService;
    private final FlashsaleItemRepository flashsaleItemRepository;

    /**
     * UC-06: Đặt vé Flash Sale (GA)
     * validate → atomicDecrease Redis → Order PENDING + hold 10p + OrderItem snapshot
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

            // Áp dụng Flash Sale Campaign nếu đang hoạt động
            Optional<FlashsaleItem> flashOpt = flashsaleItemRepository.findActiveFlashSaleItem(ticketType.getId(), now);
            if (flashOpt.isPresent() && flashOpt.get().hasAvailable(request.getQuantity())) {
                FlashsaleItem flashItem = flashOpt.get();
                unitPrice = flashItem.getFlashPrice();
                flashItem.decreaseAvailable(request.getQuantity());
                flashsaleItemRepository.save(flashItem);
                log.info("Applied flash sale price {} for ticketType {}", unitPrice, ticketType.getId());
            }

            BigDecimal total = unitPrice.multiply(BigDecimal.valueOf(request.getQuantity()));

            Order order = Order.builder()
                    .orderCode(generateOrderCode())
                    .user(user)
                    .event(event)
                    .totalAmount(total)
                    .status(OrderStatus.PENDING)
                    .expiresAt(now.plusMinutes(holdMinutes))
                    .build();

            // 6. OrderItem – snapshot unitPrice
            OrderItem item = OrderItem.builder()
                    .ticketType(ticketType)
                    .lineType(LineItemType.GA)
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

    /**
     * Phase 2: Đặt vé có chỗ ngồi (Seated Booking) theo suất diễn ShowSession
     * validate seats → acquire Redis locks → Order PENDING + hold 10p + SessionSeat LOCKED
     */
    @Transactional
    public OrderResponse createOrderSeated(Long userId, CreateSeatedOrderRequest request) {
        // 1. User
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        if (!user.canLogin()) {
            throw new AppException(ErrorCode.USER_BANNED);
        }

        // 2. ShowSession
        ShowSession session = showSessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_FOUND));
        Event event = session.getEvent();
        if (event == null || !event.isPublished()) {
            throw new AppException(ErrorCode.EVENT_NOT_PUBLISHED);
        }

        // 3. Validate Seats
        List<Long> seatIds = request.getSeatIds();
        if (seatIds == null || seatIds.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_TICKET_QUANTITY);
        }

        List<SessionSeat> sessionSeats = sessionSeatRepository.findByShowSessionIdAndSeatIdIn(session.getId(), seatIds);
        if (sessionSeats.size() != seatIds.size()) {
            throw new AppException(ErrorCode.SEAT_NOT_FOUND);
        }

        // Kiểm tra tình trạng từng ghế
        for (SessionSeat ss : sessionSeats) {
            if (!ss.isAvailable()) {
                if (ss.getSeatStatus() == SeatStatus.BOOKED) {
                    throw new AppException(ErrorCode.SEAT_BOOKED);
                }
                throw new AppException(ErrorCode.SEAT_LOCKED);
            }
            if (redisSeatLockService.isSeatLocked(session.getId(), ss.getSeat().getId())) {
                throw new AppException(ErrorCode.SEAT_LOCKED);
            }
        }

        // 4. Khóa hàng loạt ghế trên Redis (SETNX atomic, TTL = holdMinutes)
        boolean acquired = redisSeatLockService.acquireMultipleSeatLocks(
                session.getId(),
                seatIds,
                userId,
                (long) holdMinutes * 60
        );
        if (!acquired) {
            throw new AppException(ErrorCode.SEAT_LOCKED);
        }

        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expiresAt = now.plusMinutes(holdMinutes);

            Order order = Order.builder()
                    .orderCode(generateOrderCode())
                    .user(user)
                    .event(event)
                    .totalAmount(BigDecimal.ZERO)
                    .status(OrderStatus.PENDING)
                    .expiresAt(expiresAt)
                    .build();

            BigDecimal total = BigDecimal.ZERO;

            for (SessionSeat ss : sessionSeats) {
                Seat seat = ss.getSeat();
                Zone zone = seat.getZone();

                // Xác định TicketType tương ứng theo Session + Zone
                TicketType ticketType = ticketTypeRepository.findFirstByShowSessionIdAndZoneIdAndDeletedAtIsNull(session.getId(), zone.getId())
                        .or(() -> ticketTypeRepository.findFirstByZoneIdAndDeletedAtIsNull(zone.getId()))
                        .or(() -> ticketTypeRepository.findByShowSessionIdAndDeletedAtIsNull(session.getId()).stream().findFirst())
                        .orElseThrow(() -> new AppException(ErrorCode.TICKET_TYPE_NOT_FOUND));

                BigDecimal unitPrice = ticketType.getPrice();

                // Check flash campaign price
                Optional<FlashsaleItem> flashOpt = flashsaleItemRepository.findActiveFlashSaleItem(ticketType.getId(), now);
                if (flashOpt.isPresent() && flashOpt.get().hasAvailable(1)) {
                    FlashsaleItem fi = flashOpt.get();
                    unitPrice = fi.getFlashPrice();
                    fi.decreaseAvailable(1);
                    flashsaleItemRepository.save(fi);
                }

                OrderItem item = OrderItem.builder()
                        .ticketType(ticketType)
                        .lineType(LineItemType.SEATED)
                        .sessionSeat(ss)
                        .quantity(1)
                        .unitPrice(unitPrice)
                        .build();

                order.addItem(item);
                total = total.add(unitPrice);

                // Khóa ghế trong DB
                ss.lock(userId, expiresAt);
                sessionSeatRepository.save(ss);
            }

            order.setTotalAmount(total);
            Order saved = orderRepository.save(order);
            log.info("Created seated order {} with {} seats for user {}", saved.getOrderCode(), seatIds.size(), userId);
            return orderMapper.toOrderResponse(saved);
        } catch (RuntimeException ex) {
            // Rollback Redis seat locks
            redisSeatLockService.releaseMultipleSeatLocks(session.getId(), seatIds);
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