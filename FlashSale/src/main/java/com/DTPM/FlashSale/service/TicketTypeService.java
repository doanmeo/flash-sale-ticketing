package com.DTPM.FlashSale.service;

import com.DTPM.FlashSale.dto.request.CreateTicketTypeRequest;
import com.DTPM.FlashSale.dto.request.UpdateTicketTypeRequest;
import com.DTPM.FlashSale.dto.response.TicketTypeResponse;
import com.DTPM.FlashSale.domain.entity.Event;
import com.DTPM.FlashSale.domain.entity.TicketType;
import com.DTPM.FlashSale.domain.entity.User;
import com.DTPM.FlashSale.exception.AppException;
import com.DTPM.FlashSale.exception.ErrorCode;
import com.DTPM.FlashSale.infrastructure.RedisStockService;
import com.DTPM.FlashSale.mapper.TicketTypeMapper;
import com.DTPM.FlashSale.domain.repository.EventRepository;
import com.DTPM.FlashSale.domain.repository.TicketTypeRepository;
import com.DTPM.FlashSale.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketTypeService {

    private final TicketTypeRepository ticketTypeRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final RedisStockService redisStockService;
    private final TicketTypeMapper ticketTypeMapper;

    @Transactional
    public TicketTypeResponse createTicketType(Long organizerId, Long eventId, CreateTicketTypeRequest request) {
        User organizer = userRepository.findById(organizerId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Event event = eventRepository.findByIdAndDeletedAtIsNull(eventId)
                .orElseThrow(() -> new AppException(ErrorCode.EVENT_NOT_FOUND));

        if (!event.isOwnedBy(organizer)) {
            throw new AppException(ErrorCode.EVENT_ACCESS_DENIED);
        }

        validateTicketTypeRequest(request.getSaleStart(), request.getSaleEnd(),
                request.getMinPerOrder(), request.getMaxPerOrder());

        TicketType ticketType = ticketTypeMapper.toEntity(request);
        ticketType.setEvent(event);
        ticketType.setAvailableQuantity(request.getTotalQuantity());

        TicketType saved = ticketTypeRepository.save(ticketType);

        // Khởi tạo Redis inventory stock
        redisStockService.initStock(saved.getId(), saved.getTotalQuantity());

        log.info("Created TicketType id={} for Event id={}, initialized Redis stock={}",
                saved.getId(), eventId, saved.getTotalQuantity());

        return enrichRealtimeStock(ticketTypeMapper.toTicketTypeResponse(saved));
    }

    @Transactional
    public TicketTypeResponse updateTicketType(Long organizerId, Long ticketTypeId, UpdateTicketTypeRequest request) {
        User organizer = userRepository.findById(organizerId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        TicketType ticketType = ticketTypeRepository.findByIdAndDeletedAtIsNull(ticketTypeId)
                .orElseThrow(() -> new AppException(ErrorCode.TICKET_TYPE_NOT_FOUND));

        if (!ticketType.getEvent().isOwnedBy(organizer)) {
            throw new AppException(ErrorCode.EVENT_ACCESS_DENIED);
        }

        LocalDateTime saleStart = request.getSaleStart() != null ? request.getSaleStart() : ticketType.getSaleStart();
        LocalDateTime saleEnd = request.getSaleEnd() != null ? request.getSaleEnd() : ticketType.getSaleEnd();
        Integer minPerOrder = request.getMinPerOrder() != null ? request.getMinPerOrder() : ticketType.getMinPerOrder();
        Integer maxPerOrder = request.getMaxPerOrder() != null ? request.getMaxPerOrder() : ticketType.getMaxPerOrder();

        validateTicketTypeRequest(saleStart, saleEnd, minPerOrder, maxPerOrder);

        ticketTypeMapper.updateEntityFromRequest(request, ticketType);

        if (request.getTotalQuantity() != null) {
            // Nếu cập nhật totalQuantity, đồng bộ lại availableQuantity và Redis
            ticketType.setAvailableQuantity(request.getTotalQuantity());
            redisStockService.initStock(ticketType.getId(), request.getTotalQuantity());
        }

        TicketType updated = ticketTypeRepository.save(ticketType);
        return enrichRealtimeStock(ticketTypeMapper.toTicketTypeResponse(updated));
    }

    @Transactional
    public void softDeleteTicketType(Long organizerId, Long ticketTypeId) {
        User organizer = userRepository.findById(organizerId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        TicketType ticketType = ticketTypeRepository.findByIdAndDeletedAtIsNull(ticketTypeId)
                .orElseThrow(() -> new AppException(ErrorCode.TICKET_TYPE_NOT_FOUND));

        if (!ticketType.getEvent().isOwnedBy(organizer)) {
            throw new AppException(ErrorCode.EVENT_ACCESS_DENIED);
        }

        ticketType.softDelete();
        ticketTypeRepository.save(ticketType);
        redisStockService.deleteStock(ticketTypeId);
        log.info("Soft deleted TicketType id={}", ticketTypeId);
    }

    @Transactional(readOnly = true)
    public List<TicketTypeResponse> getTicketTypesByEventId(Long eventId) {
        List<TicketType> list = ticketTypeRepository.findByEventIdAndIsHiddenFalseAndDeletedAtIsNull(eventId);
        return list.stream()
                .map(ticketTypeMapper::toTicketTypeResponse)
                .map(this::enrichRealtimeStock)
                .toList();
    }

    public TicketTypeResponse enrichRealtimeStock(TicketTypeResponse response) {
        int realtime = redisStockService.getAvailable(response.getId());
        response.setRealtimeAvailable(realtime);
        return response;
    }

    private void validateTicketTypeRequest(LocalDateTime start, LocalDateTime end, int min, int max) {
        if (start != null && end != null && !start.isBefore(end)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Sale start time must be before sale end time");
        }
        if (min > max) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Min per order cannot be greater than max per order");
        }
        if (min < 1) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Min per order must be at least 1");
        }
    }
}
