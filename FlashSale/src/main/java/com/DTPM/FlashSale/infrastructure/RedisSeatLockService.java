package com.DTPM.FlashSale.infrastructure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisSeatLockService {

    private static final String SEAT_KEY_PREFIX = "seat:";
    private final StringRedisTemplate redisTemplate;

    private String seatKey(Long sessionId, Long seatId) {
        return SEAT_KEY_PREFIX + sessionId + ":" + seatId;
    }

    /**
     * Thử acquire lock nguyên tử cho 1 ghế bằng Redis SETNX + EXPIRE
     */
    public boolean acquireSeatLock(Long sessionId, Long seatId, Long userId, long ttlSeconds) {
        String key = seatKey(sessionId, seatId);
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(key, String.valueOf(userId), Duration.ofSeconds(ttlSeconds));
        return Boolean.TRUE.equals(success);
    }

    /**
     * Acquire lock đồng thời cho danh sách ghế. Nếu 1 ghế thất bại -> rollback toàn bộ các ghế đã acquire trước đó.
     */
    public boolean acquireMultipleSeatLocks(Long sessionId, List<Long> seatIds, Long userId, long ttlSeconds) {
        List<Long> lockedSoFar = new ArrayList<>();
        for (Long seatId : seatIds) {
            boolean ok = acquireSeatLock(sessionId, seatId, userId, ttlSeconds);
            if (ok) {
                lockedSoFar.add(seatId);
            } else {
                // Rollback các ghế đã lock trước đó trong lượt này
                for (Long lockedId : lockedSoFar) {
                    releaseSeatLock(sessionId, lockedId);
                }
                return false;
            }
        }
        return true;
    }

    /**
     * Giải phóng lock ghế trên Redis khi đơn bị hủy, hết hạn hoặc thanh toán thành công
     */
    public void releaseSeatLock(Long sessionId, Long seatId) {
        redisTemplate.delete(seatKey(sessionId, seatId));
    }

    public void releaseMultipleSeatLocks(Long sessionId, List<Long> seatIds) {
        if (seatIds == null || seatIds.isEmpty()) return;
        List<String> keys = seatIds.stream().map(id -> seatKey(sessionId, id)).toList();
        redisTemplate.delete(keys);
    }

    public boolean isSeatLocked(Long sessionId, Long seatId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(seatKey(sessionId, seatId)));
    }
}
