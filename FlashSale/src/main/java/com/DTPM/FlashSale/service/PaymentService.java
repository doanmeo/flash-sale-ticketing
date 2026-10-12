package com.DTPM.FlashSale.service;

import com.DTPM.FlashSale.dto.response.PaymentResponse;
import com.DTPM.FlashSale.domain.entity.Order;
import com.DTPM.FlashSale.domain.entity.Payment;
import com.DTPM.FlashSale.domain.enums.OrderStatus;
import com.DTPM.FlashSale.exception.AppException;
import com.DTPM.FlashSale.exception.ErrorCode;
import com.DTPM.FlashSale.infrastructure.QrCodeService;
import com.DTPM.FlashSale.mapper.PaymentMapper;
import com.DTPM.FlashSale.domain.repository.OrderRepository;
import com.DTPM.FlashSale.domain.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final QrCodeService qrCodeService;
    private final PaymentMapper paymentMapper;
    private final TicketInstanceService ticketInstanceService;
    private final com.DTPM.FlashSale.domain.repository.SessionSeatRepository sessionSeatRepository;
    private final com.DTPM.FlashSale.infrastructure.RedisSeatLockService redisSeatLockService;

    /**
     * UC-07: Thanh toán giả lập (Mock Payment) cho đơn hàng PENDING
     */
    @Transactional
    public PaymentResponse pay(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getUser().getId().equals(userId)) {
            throw new AppException(ErrorCode.ORDER_ACCESS_DENIED);
        }

        if (paymentRepository.findByOrderId(orderId).isPresent() || order.getPayment() != null) {
            throw new AppException(ErrorCode.PAYMENT_ALREADY_EXISTS);
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new AppException(ErrorCode.ORDER_NOT_PENDING);
        }

        if (order.isExpired()) {
            throw new AppException(ErrorCode.ORDER_EXPIRED);
        }

        if (!order.canBePaid()) {
            throw new AppException(ErrorCode.ORDER_CANNOT_PAY);
        }

        // Đánh dấu đơn hàng là PAID
        order.markPaid();
        orderRepository.save(order);

        // Chuyển trạng thái ghế SEATED thành BOOKED & giải phóng lock Redis
        if (order.getItems() != null) {
            for (com.DTPM.FlashSale.domain.entity.OrderItem item : order.getItems()) {
                if (item.getSessionSeat() != null) {
                    com.DTPM.FlashSale.domain.entity.SessionSeat ss = item.getSessionSeat();
                    ss.book();
                    sessionSeatRepository.save(ss);
                    if (ss.getShowSession() != null && ss.getSeat() != null) {
                        redisSeatLockService.releaseSeatLock(ss.getShowSession().getId(), ss.getSeat().getId());
                    }
                }
            }
        }

        // Tạo bản ghi Payment thành công (Mock)
        Payment payment = Payment.createSuccess(order, order.getTotalAmount());
        Payment savedPayment = paymentRepository.save(payment);

        // Phát hành các bản ghi vé điện tử TicketInstance
        ticketInstanceService.issueTicketsForOrder(order);

        log.info("Order {} (id={}) successfully paid by user {}", order.getOrderCode(), orderId, userId);

        PaymentResponse response = paymentMapper.toPaymentResponse(savedPayment);
        response.setQrCodeUrl(qrCodeService.generate(order.getOrderCode()));

        return response;
    }
}
