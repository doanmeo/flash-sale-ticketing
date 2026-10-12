package com.DTPM.FlashSale.service;

import com.DTPM.FlashSale.dto.request.CreateShowSessionRequest;
import com.DTPM.FlashSale.dto.response.SessionSeatResponse;
import com.DTPM.FlashSale.dto.response.ShowSessionResponse;
import com.DTPM.FlashSale.dto.response.TicketTypeResponse;
import com.DTPM.FlashSale.domain.entity.*;
import com.DTPM.FlashSale.domain.enums.SeatStatus;
import com.DTPM.FlashSale.domain.enums.SessionStatus;
import com.DTPM.FlashSale.exception.AppException;
import com.DTPM.FlashSale.exception.ErrorCode;
import com.DTPM.FlashSale.infrastructure.RedisSeatLockService;
import com.DTPM.FlashSale.infrastructure.RedisStockService;
import com.DTPM.FlashSale.mapper.TicketTypeMapper;
import com.DTPM.FlashSale.domain.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShowSessionService {

    private final ShowSessionRepository showSessionRepository;
    private final SessionSeatRepository sessionSeatRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final SeatRepository seatRepository;
    private final RedisSeatLockService redisSeatLockService;
    private final RedisStockService redisStockService;
    private final TicketTypeMapper ticketTypeMapper;

    /**
     * Lấy danh sách suất diễn (ShowSession) của một Event
     */
    @Transactional(readOnly = true)
    public List<ShowSessionResponse> getSessionsByEvent(Long eventId) {
        Event event = eventRepository.findByIdAndDeletedAtIsNull(eventId)
                .orElseThrow(() -> new AppException(ErrorCode.EVENT_NOT_FOUND));

        List<ShowSession> sessions = showSessionRepository.findByEventId(eventId);
        return sessions.stream().map(this::toShowSessionResponse).collect(Collectors.toList());
    }

    /**
     * Lấy Seat Map (sơ đồ ghế) cho 1 suất diễn ShowSession kèm trạng thái realtime
     */
    @Transactional(readOnly = true)
    public List<SessionSeatResponse> getSeatMap(Long sessionId) {
        ShowSession session = showSessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_FOUND));

        List<SessionSeat> sessionSeats = sessionSeatRepository.findByShowSessionId(sessionId);
        if (sessionSeats.isEmpty()) {
            return Collections.emptyList();
        }

        // Cache map TicketType per Zone for price resolution
        List<TicketType> ticketTypes = ticketTypeRepository.findByShowSessionIdAndDeletedAtIsNull(sessionId);
        Map<Long, BigDecimal> zonePriceMap = ticketTypes.stream()
                .filter(t -> t.getZone() != null)
                .collect(Collectors.toMap(t -> t.getZone().getId(), TicketType::getPrice, (p1, p2) -> p1));

        return sessionSeats.stream().map(ss -> {
            Seat seat = ss.getSeat();
            Zone zone = seat != null ? seat.getZone() : null;
            Long zoneId = zone != null ? zone.getId() : null;
            String zoneName = zone != null ? zone.getZoneName() : null;
            Boolean isSeated = zone != null ? zone.getIsSeated() : true;

            // Xác định trạng thái ghế realtime
            SeatStatus status = ss.getSeatStatus();
            if (status == SeatStatus.LOCKED) {
                if (ss.getLockedUntil() != null && ss.getLockedUntil().isBefore(LocalDateTime.now())) {
                    status = SeatStatus.AVAILABLE;
                } else if (!redisSeatLockService.isSeatLocked(sessionId, seat.getId())) {
                    // Lock hết hạn trên Redis
                    status = SeatStatus.AVAILABLE;
                }
            } else if (status == SeatStatus.AVAILABLE && redisSeatLockService.isSeatLocked(sessionId, seat.getId())) {
                status = SeatStatus.LOCKED;
            }

            BigDecimal price = zoneId != null ? zonePriceMap.getOrDefault(zoneId, BigDecimal.ZERO) : BigDecimal.ZERO;

            return SessionSeatResponse.builder()
                    .sessionSeatId(ss.getId())
                    .seatId(seat != null ? seat.getId() : null)
                    .rowLabel(seat != null ? seat.getRowLabel() : "")
                    .seatNumber(seat != null ? seat.getSeatNumber() : null)
                    .zoneId(zoneId)
                    .zoneName(zoneName)
                    .isSeated(isSeated)
                    .status(status)
                    .price(price)
                    .build();
        }).collect(Collectors.toList());
    }

    /**
     * Organizer tạo suất diễn ShowSession mới cho Event của mình
     */
    @Transactional
    public ShowSessionResponse createSession(Long organizerId, Long eventId, CreateShowSessionRequest request) {
        User organizer = userRepository.findById(organizerId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Event event = eventRepository.findByIdAndDeletedAtIsNull(eventId)
                .orElseThrow(() -> new AppException(ErrorCode.EVENT_NOT_FOUND));

        if (!event.isOwnedBy(organizer)) {
            throw new AppException(ErrorCode.EVENT_ACCESS_DENIED);
        }

        ShowSession session = ShowSession.builder()
                .event(event)
                .sessionName(request.getSessionName())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .status(SessionStatus.SCHEDULED)
                .build();

        ShowSession saved = showSessionRepository.save(session);

        // Tự động sinh SessionSeat nếu Venue của Event có các Seat
        if (event.getVenue() != null && event.getVenue().getZones() != null) {
            for (Zone zone : event.getVenue().getZones()) {
                if (Boolean.TRUE.equals(zone.getIsSeated())) {
                    List<Seat> seats = seatRepository.findByZoneId(zone.getId());
                    for (Seat seat : seats) {
                        SessionSeat ss = SessionSeat.builder()
                                .showSession(saved)
                                .seat(seat)
                                .seatStatus(SeatStatus.AVAILABLE)
                                .build();
                        sessionSeatRepository.save(ss);
                    }
                }
            }
        }

        log.info("Organizer {} created session {} for event {}", organizerId, saved.getId(), eventId);
        return toShowSessionResponse(saved);
    }

    public ShowSessionResponse toShowSessionResponse(ShowSession session) {
        List<TicketTypeResponse> ticketTypeResponses = session.getTicketTypes() != null
                ? session.getTicketTypes().stream()
                .filter(t -> t.getDeletedAt() == null)
                .map(t -> {
                    TicketTypeResponse res = ticketTypeMapper.toTicketTypeResponse(t);
                    res.setRealtimeAvailable(redisStockService.getAvailable(t.getId()));
                    return res;
                })
                .collect(Collectors.toList())
                : Collections.emptyList();

        return ShowSessionResponse.builder()
                .id(session.getId())
                .eventId(session.getEvent() != null ? session.getEvent().getId() : null)
                .sessionName(session.getSessionName())
                .startTime(session.getStartTime())
                .endTime(session.getEndTime())
                .status(session.getStatus())
                .ticketTypes(ticketTypeResponses)
                .build();
    }
}
