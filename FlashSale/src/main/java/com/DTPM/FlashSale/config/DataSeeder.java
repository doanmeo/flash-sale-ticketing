package com.DTPM.FlashSale.config;

import com.DTPM.FlashSale.domain.entity.*;
import com.DTPM.FlashSale.domain.enums.*;
import com.DTPM.FlashSale.domain.repository.*;
import com.DTPM.FlashSale.infrastructure.RedisStockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final LocationRepository locationRepository;
    private final VenueRepository venueRepository;
    private final ZoneRepository zoneRepository;
    private final SeatRepository seatRepository;
    private final ShowSessionRepository showSessionRepository;
    private final SessionSeatRepository sessionSeatRepository;
    private final FlashsaleCampaignRepository flashsaleCampaignRepository;
    private final FlashsaleItemRepository flashsaleItemRepository;
    private final PasswordEncoder passwordEncoder;
    private final RedisStockService redisStockService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            log.info("Seed skipped: data already exists");
            return;
        }

        User admin = userRepository.save(User.builder()
                .email("admin@flashsale.local")
                .passwordHash(passwordEncoder.encode("Admin@123"))
                .fullName("System Admin")
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .build());

        User organizer = userRepository.save(User.builder()
                .email("organizer@flashsale.local")
                .passwordHash(passwordEncoder.encode("Organizer@123"))
                .fullName("Demo Organizer")
                .role(UserRole.ORGANIZER)
                .status(UserStatus.ACTIVE)
                .build());

        User user = userRepository.save(User.builder()
                .email("user@flashsale.local")
                .passwordHash(passwordEncoder.encode("User@123"))
                .fullName("Demo User")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());

        LocalDateTime now = LocalDateTime.now();

        // 1. Location & Venue
        Location location = locationRepository.save(Location.builder()
                .locationName("Khu vực Hà Nội")
                .city("Hà Nội")
                .district("Nam Từ Liêm")
                .build());

        Venue venue = venueRepository.save(Venue.builder()
                .venueName("Trung Tâm Hội Nghị Quốc Gia")
                .address("Đại lộ Thăng Long, Mễ Trì, Nam Từ Liêm, Hà Nội")
                .totalCapacity(5000)
                .location(location)
                .build());

        // 2. Zones (1 Seated, 1 GA)
        Zone vipZone = zoneRepository.save(Zone.builder()
                .zoneName("VIP Diamond (Seated)")
                .capacity(30)
                .isSeated(true)
                .zoneRank(1)
                .venue(venue)
                .build());

        Zone gaZone = zoneRepository.save(Zone.builder()
                .zoneName("GA Standing Zone")
                .capacity(200)
                .isSeated(false)
                .zoneRank(2)
                .venue(venue)
                .build());

        // 3. Seats in VIP Zone (Rows A, B, C; 10 seats each)
        List<Seat> seats = new ArrayList<>();
        String[] rows = {"A", "B", "C"};
        for (String row : rows) {
            for (int num = 1; num <= 10; num++) {
                Seat seat = Seat.builder()
                        .zone(vipZone)
                        .rowLabel(row)
                        .seatNumber(String.valueOf(num))
                        .isActive(true)
                        .build();
                seats.add(seat);
            }
        }
        seats = seatRepository.saveAll(seats);

        // 4. Event
        Event event = eventRepository.save(Event.builder()
                .name("Flash Sale Concert Demo")
                .description("Sự kiện demo flash sale Hybrid (GA + Seated) cho đồ án")
                .location("Hà Nội")
                .eventDate(now.plusDays(30))
                .imageUrl(null)
                .status(EventStatus.PUBLISHED)
                .organizer(organizer)
                .venue(venue)
                .build());

        // 5. ShowSession
        ShowSession session = showSessionRepository.save(ShowSession.builder()
                .event(event)
                .sessionName("Đêm Nhạc 19:30 - Grand Opening")
                .startTime(now.plusDays(30).withHour(19).withMinute(30))
                .endTime(now.plusDays(30).withHour(22).withMinute(30))
                .status(SessionStatus.SELLING)
                .build());

        // 6. SessionSeats for the session
        List<SessionSeat> sessionSeats = new ArrayList<>();
        for (Seat seat : seats) {
            sessionSeats.add(SessionSeat.builder()
                    .showSession(session)
                    .seat(seat)
                    .seatStatus(SeatStatus.AVAILABLE)
                    .build());
        }
        sessionSeatRepository.saveAll(sessionSeats);

        // 7. TicketTypes
        TicketType vip = ticketTypeRepository.save(TicketType.builder()
                .name("VIP Diamond Seat")
                .description("Vé ghế VIP kèm quà tặng")
                .price(new BigDecimal("1500000"))
                .totalQuantity(30)
                .availableQuantity(30)
                .saleStart(now.minusHours(1))
                .saleEnd(now.plusDays(7))
                .minPerOrder(1)
                .maxPerOrder(4)
                .isHidden(false)
                .event(event)
                .showSession(session)
                .zone(vipZone)
                .build());

        TicketType standard = ticketTypeRepository.save(TicketType.builder()
                .name("Standard GA")
                .description("Vé đứng khu GA")
                .price(new BigDecimal("500000"))
                .totalQuantity(200)
                .availableQuantity(200)
                .saleStart(now.minusHours(1))
                .saleEnd(now.plusDays(7))
                .minPerOrder(1)
                .maxPerOrder(6)
                .isHidden(false)
                .event(event)
                .showSession(session)
                .zone(gaZone)
                .build());

        // 8. Flashsale Campaign & Items
        FlashsaleCampaign campaign = flashsaleCampaignRepository.save(FlashsaleCampaign.builder()
                .campaignName("Flash Sale Mở Bán Sớm Giờ Vàng")
                .startAt(now.minusHours(1))
                .endAt(now.plusDays(2))
                .status(CampaignStatus.ACTIVE)
                .maxPerUser(2)
                .build());

        flashsaleItemRepository.save(FlashsaleItem.builder()
                .campaign(campaign)
                .ticketType(vip)
                .flashPrice(new BigDecimal("1200000"))
                .flashQuota(15)
                .flashAvailable(15)
                .perUserLimit(2)
                .build());

        flashsaleItemRepository.save(FlashsaleItem.builder()
                .campaign(campaign)
                .ticketType(standard)
                .flashPrice(new BigDecimal("350000"))
                .flashQuota(50)
                .flashAvailable(50)
                .perUserLimit(4)
                .build());

        // 9. Init Redis stock for GA
        redisStockService.initStock(vip.getId(), vip.getTotalQuantity());
        redisStockService.initStock(standard.getId(), standard.getTotalQuantity());

        log.info("==================== SEED COMPLETED ====================");
        log.info("Admin account:     admin@flashsale.local / Admin@123 (ROLE_ADMIN)");
        log.info("Organizer account: organizer@flashsale.local / Organizer@123 (ROLE_ORGANIZER)");
        log.info("User account:      user@flashsale.local / User@123 (ROLE_USER)");
        log.info("Sample Venue:      id={}, name='{}', capacity={}", venue.getId(), venue.getVenueName(), venue.getTotalCapacity());
        log.info("Sample Event:      id={}, name='{}', status=PUBLISHED", event.getId(), event.getName());
        log.info("Sample Session:    id={}, name='{}', status={}", session.getId(), session.getSessionName(), session.getStatus());
        log.info("Seated Seats:      {} seats generated in Zone '{}'", seats.size(), vipZone.getZoneName());
        log.info("Ticket VIP:        id={}, price=1500000, flashPrice=1200000", vip.getId());
        log.info("Ticket Standard:   id={}, price=500000, flashPrice=350000", standard.getId());
        log.info("Flash Campaign:    id={}, name='{}', status=ACTIVE", campaign.getId(), campaign.getCampaignName());
        log.info("========================================================");
    }
}