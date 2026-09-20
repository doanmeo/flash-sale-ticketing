package com.DTPM.FlashSale.infrastructure;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Quản lý inventory trên Redis.
 * Key: ticket:{ticketTypeId}:available
 */
@Service
@RequiredArgsConstructor
public class RedisStockService {

    private static final String KEY_PREFIX = "ticket:";
    private static final String KEY_SUFFIX = ":available";

    private final StringRedisTemplate redisTemplate;

    private String key(Long ticketTypeId) {
        return KEY_PREFIX + ticketTypeId + KEY_SUFFIX;
    }

    /** Khởi tạo / ghi đè số lượng khi tạo hoặc sync TicketType */
    public void initStock(Long ticketTypeId, int quantity) {
        redisTemplate.opsForValue().set(key(ticketTypeId), String.valueOf(quantity));
    }

    public int getAvailable(Long ticketTypeId) {
        String val = redisTemplate.opsForValue().get(key(ticketTypeId));
        if (val == null) {
            return 0;
        }
        return Integer.parseInt(val);
    }

    /**
     * Atomic decrease. Trả true nếu đủ vé; false nếu sold out (đã rollback).
     */
    public boolean atomicDecrease(Long ticketTypeId, int quantity) {
        if (quantity <= 0) {
            return false;
        }
        Long remain = redisTemplate.opsForValue().decrement(key(ticketTypeId), quantity);
        if (remain == null) {
            return false;
        }
        if (remain < 0) {
            // Rollback
            redisTemplate.opsForValue().increment(key(ticketTypeId), quantity);
            return false;
        }
        return true;
    }

    /** Trả lại inventory khi hủy / hết hạn hold */
    public void increase(Long ticketTypeId, int quantity) {
        if (quantity <= 0) {
            return;
        }
        redisTemplate.opsForValue().increment(key(ticketTypeId), quantity);
    }

    public void deleteStock(Long ticketTypeId) {
        redisTemplate.delete(key(ticketTypeId));
    }
}