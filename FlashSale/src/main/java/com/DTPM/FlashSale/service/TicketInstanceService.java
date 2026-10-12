package com.DTPM.FlashSale.service;

import com.DTPM.FlashSale.domain.entity.*;
import com.DTPM.FlashSale.domain.enums.LineItemType;
import com.DTPM.FlashSale.domain.enums.TicketInstanceStatus;
import com.DTPM.FlashSale.domain.repository.TicketInstanceRepository;
import com.DTPM.FlashSale.dto.response.TicketInstanceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketInstanceService {

    private final TicketInstanceRepository ticketInstanceRepository;

    @Transactional
    public List<TicketInstance> issueTicketsForOrder(Order order) {
        List<TicketInstance> instances = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (OrderItem item : order.getItems()) {
            if (item.getLineType() == LineItemType.SEATED && item.getSessionSeat() != null) {
                // Seated item: 1 ticket per item
                Seat seat = item.getSessionSeat().getSeat();
                String seatLabel = seat != null ? (seat.getRowLabel() + "-" + seat.getSeatNumber()) : "SEAT";
                String code = generateTicketCode(order.getId(), item.getId(), 1);
                String qr = String.format("TICKET:%s|ORDER:%s|USER:%d|EVENT:%s|SEAT:%s",
                        code, order.getOrderCode(), order.getUser().getId(),
                        order.getEvent() != null ? order.getEvent().getName() : "", seatLabel);

                TicketInstance instance = TicketInstance.builder()
                        .ticketCode(code)
                        .qrPayload(qr)
                        .status(TicketInstanceStatus.VALID)
                        .issuedAt(now)
                        .orderItem(item)
                        .user(order.getUser())
                        .build();
                instances.add(ticketInstanceRepository.save(instance));
            } else {
                // GA item: issue 1 ticket per quantity
                int qty = item.getQuantity() != null ? item.getQuantity() : 1;
                String typeName = item.getTicketType() != null ? item.getTicketType().getName() : "GA";
                for (int i = 1; i <= qty; i++) {
                    String code = generateTicketCode(order.getId(), item.getId(), i);
                    String qr = String.format("TICKET:%s|ORDER:%s|USER:%d|EVENT:%s|TYPE:%s",
                            code, order.getOrderCode(), order.getUser().getId(),
                            order.getEvent() != null ? order.getEvent().getName() : "", typeName);

                    TicketInstance instance = TicketInstance.builder()
                            .ticketCode(code)
                            .qrPayload(qr)
                            .status(TicketInstanceStatus.VALID)
                            .issuedAt(now)
                            .orderItem(item)
                            .user(order.getUser())
                            .build();
                    instances.add(ticketInstanceRepository.save(instance));
                }
            }
        }

        log.info("Issued {} ticket instances for order {}", instances.size(), order.getOrderCode());
        return instances;
    }

    @Transactional(readOnly = true)
    public List<TicketInstanceResponse> getMyTickets(Long userId) {
        List<TicketInstance> instances = ticketInstanceRepository.findByUserIdOrderByIssuedAtDesc(userId);
        return instances.stream().map(this::toResponse).toList();
    }

    private TicketInstanceResponse toResponse(TicketInstance instance) {
        OrderItem item = instance.getOrderItem();
        Order order = item != null ? item.getOrder() : null;
        Event event = order != null ? order.getEvent() : null;

        String sessionName = null;
        String zoneName = null;
        String seatInfo = null;

        if (item != null && item.getSessionSeat() != null) {
            SessionSeat ss = item.getSessionSeat();
            if (ss.getShowSession() != null) sessionName = ss.getShowSession().getSessionName();
            if (ss.getSeat() != null) {
                seatInfo = "Row " + ss.getSeat().getRowLabel() + " - Seat " + ss.getSeat().getSeatNumber();
                if (ss.getSeat().getZone() != null) zoneName = ss.getSeat().getZone().getZoneName();
            }
        } else if (item != null && item.getTicketType() != null) {
            TicketType tt = item.getTicketType();
            if (tt.getShowSession() != null) sessionName = tt.getShowSession().getSessionName();
            if (tt.getZone() != null) zoneName = tt.getZone().getZoneName();
        }

        return TicketInstanceResponse.builder()
                .id(instance.getId())
                .ticketCode(instance.getTicketCode())
                .qrPayload(instance.getQrPayload())
                .status(instance.getStatus())
                .lineType(item != null ? item.getLineType() : LineItemType.GA)
                .eventName(event != null ? event.getName() : null)
                .sessionName(sessionName)
                .zoneName(zoneName)
                .seatInfo(seatInfo)
                .price(item != null ? item.getUnitPrice() : null)
                .issuedAt(instance.getIssuedAt())
                .build();
    }

    private String generateTicketCode(Long orderId, Long itemId, int idx) {
        String rand = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return String.format("TI-%d-%d-%d-%s", orderId, itemId, idx, rand);
    }
}
