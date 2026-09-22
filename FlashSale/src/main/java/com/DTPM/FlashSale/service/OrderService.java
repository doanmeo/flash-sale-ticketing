package com.DTPM.FlashSale.service;

import com.DTPM.FlashSale.dto.response.OrderResponse;
import com.DTPM.FlashSale.entity.Event;
import com.DTPM.FlashSale.entity.Order;
import com.DTPM.FlashSale.entity.OrderItem;
import com.DTPM.FlashSale.entity.TicketType;
import com.DTPM.FlashSale.entity.User;
import com.DTPM.FlashSale.enums.OrderStatus;
import com.DTPM.FlashSale.enums.UserRole;
import com.DTPM.FlashSale.exception.AppException;
import com.DTPM.FlashSale.exception.ErrorCode;
import com.DTPM.FlashSale.infrastructure.RedisStockService;
import com.DTPM.FlashSale.mapper.OrderMapper;
import com.DTPM.FlashSale.repository.EventRepository;
import com.DTPM.FlashSale.repository.OrderRepository;
import com.DTPM.FlashSale.repository.TicketTypeRepository;
import com.DTPM.FlashSale.repository.UserRepository;
import com.DTPM.FlashSale.security.SecurityUtils;
import com.DTPM.FlashSale.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final RedisStockService redisStockService;
    private final OrderMapper orderMapper;

    /**
     * Lấy danh sách đơn hàng thuộc sự kiện của Organizer
     */
    @Transactional(readOnly = true)
    public Page<OrderResponse> getEventOrders(Long organizerId, Long eventId, OrderStatus status, Pageable pageable) {
        User organizer = userRepository.findById(organizerId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Event event = eventRepository.findByIdAndDeletedAtIsNull(eventId)
                .orElseThrow(() -> new AppException(ErrorCode.EVENT_NOT_FOUND));

        if (!event.isOwnedBy(organizer)) {
            throw new AppException(ErrorCode.EVENT_ACCESS_DENIED);
        }

        Page<Order> page;
        if (status != null) {
            page = orderRepository.findByEventIdAndStatus(eventId, status, pageable);
        } else {
            page = orderRepository.findByEventId(eventId, pageable);
        }

        return page.map(orderMapper::toOrderResponse);
    }

    /**
     * Xuất danh sách đơn hàng dạng CSV cho Organizer
     */
    @Transactional(readOnly = true)
    public byte[] exportEventOrdersToCsv(Long organizerId, Long eventId) {
        User organizer = userRepository.findById(organizerId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Event event = eventRepository.findByIdAndDeletedAtIsNull(eventId)
                .orElseThrow(() -> new AppException(ErrorCode.EVENT_NOT_FOUND));

        if (!event.isOwnedBy(organizer)) {
            throw new AppException(ErrorCode.EVENT_ACCESS_DENIED);
        }

        List<Order> orders = orderRepository.findByEventId(eventId);

        StringBuilder csv = new StringBuilder();
        csv.append("orderCode,email,ticket,qty,amount,status,createdAt\n");

        for (Order o : orders) {
            String orderCode = escapeCsv(o.getOrderCode());
            String email = escapeCsv(o.getUser() != null ? o.getUser().getEmail() : "");

            String ticketNames = o.getItems().stream()
                    .map(i -> i.getTicketType().getName() + " (x" + i.getQuantity() + ")")
                    .collect(Collectors.joining("; "));
            String ticket = escapeCsv(ticketNames);

            int totalQty = o.getItems().stream().mapToInt(OrderItem::getQuantity).sum();
            String amount = o.getTotalAmount().toString();
            String statusStr = o.getStatus().name();
            String createdAt = o.getCreatedAt() != null ? o.getCreatedAt().toString() : "";

            csv.append(orderCode).append(",")
                    .append(email).append(",")
                    .append(ticket).append(",")
                    .append(totalQty).append(",")
                    .append(amount).append(",")
                    .append(statusStr).append(",")
                    .append(createdAt).append("\n");
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escapeCsv(String data) {
        if (data == null) return "";
        String escaped = data.replaceAll("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\n") || escaped.contains("\"")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    /**
     * Lấy danh sách lịch sử đơn hàng của User đang đăng nhập
     */
    @Transactional(readOnly = true)
    public Page<OrderResponse> getMyOrders(Long userId, OrderStatus status, Pageable pageable) {
        Page<Order> page;
        if (status != null) {
            page = orderRepository.findByUserIdAndStatus(userId, status, pageable);
        } else {
            page = orderRepository.findByUserId(userId, pageable);
        }
        return page.map(orderMapper::toOrderResponse);
    }

    /**
     * Lấy chi tiết đơn hàng (Chủ đơn hoặc Organizer của Event hoặc Admin)
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long currentUserId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        boolean isBuyer = order.getUser().getId().equals(currentUserId);
        boolean isOrganizer = order.getEvent() != null
                && order.getEvent().getOrganizer() != null
                && order.getEvent().getOrganizer().getId().equals(currentUserId);

        boolean isAdmin = false;
        try {
            UserPrincipal principal = SecurityUtils.getCurrentUserPrincipal();
            if (principal != null && principal.getRole() == UserRole.ADMIN) {
                isAdmin = true;
            }
        } catch (Exception ignored) {
            // SecurityContext not set (e.g. unit test context)
        }

        if (!isBuyer && !isOrganizer && !isAdmin) {
            throw new AppException(ErrorCode.ORDER_ACCESS_DENIED);
        }

        return orderMapper.toOrderResponse(order);
    }

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
        return orderMapper.toOrderResponse(saved);
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
}