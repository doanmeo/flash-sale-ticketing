package com.DTPM.FlashSale.service;

import com.DTPM.FlashSale.dto.response.AdminStatsResponse;
import com.DTPM.FlashSale.dto.response.OrganizerEventStatsResponse;
import com.DTPM.FlashSale.entity.Event;
import com.DTPM.FlashSale.entity.TicketType;
import com.DTPM.FlashSale.entity.User;
import com.DTPM.FlashSale.exception.AppException;
import com.DTPM.FlashSale.exception.ErrorCode;
import com.DTPM.FlashSale.infrastructure.RedisStockService;
import com.DTPM.FlashSale.repository.EventRepository;
import com.DTPM.FlashSale.repository.OrderItemRepository;
import com.DTPM.FlashSale.repository.OrderRepository;
import com.DTPM.FlashSale.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatsService {

    private final EventRepository eventRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final RedisStockService redisStockService;

    @Transactional(readOnly = true)
    public OrganizerEventStatsResponse getOrganizerEventStats(Long organizerId, Long eventId) {
        User organizer = userRepository.findById(organizerId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Event event = eventRepository.findByIdAndDeletedAtIsNull(eventId)
                .orElseThrow(() -> new AppException(ErrorCode.EVENT_NOT_FOUND));

        if (!event.isOwnedBy(organizer)) {
            throw new AppException(ErrorCode.EVENT_ACCESS_DENIED);
        }

        long paidOrdersCount = orderRepository.countPaidOrdersByEventId(eventId);
        BigDecimal totalRevenue = orderRepository.sumTotalPaidRevenueByEventId(eventId);

        List<OrganizerEventStatsResponse.TicketTypeStat> ticketStats = new ArrayList<>();
        if (event.getTicketTypes() != null) {
            for (TicketType t : event.getTicketTypes()) {
                if (t.getDeletedAt() == null) {
                    int soldQty = orderItemRepository.sumSoldQuantityByTicketTypeId(t.getId());
                    int remaining = redisStockService.getAvailable(t.getId());

                    ticketStats.add(OrganizerEventStatsResponse.TicketTypeStat.builder()
                            .ticketTypeId(t.getId())
                            .ticketTypeName(t.getName())
                            .price(t.getPrice())
                            .totalQuantity(t.getTotalQuantity())
                            .soldQuantity(soldQty)
                            .remainingQuantity(remaining)
                            .build());
                }
            }
        }

        return OrganizerEventStatsResponse.builder()
                .eventId(event.getId())
                .eventName(event.getName())
                .totalPaidOrders(paidOrdersCount)
                .totalRevenue(totalRevenue)
                .ticketTypeStats(ticketStats)
                .build();
    }

    @Transactional(readOnly = true)
    public AdminStatsResponse getAdminStats() {
        long totalUsers = userRepository.count();
        long totalEvents = eventRepository.count();
        long totalPaidOrders = orderRepository.countPaidOrders();
        BigDecimal totalRevenue = orderRepository.sumTotalPaidRevenue();

        return AdminStatsResponse.builder()
                .totalUsers(totalUsers)
                .totalEvents(totalEvents)
                .totalPaidOrders(totalPaidOrders)
                .totalRevenue(totalRevenue)
                .build();
    }
}
