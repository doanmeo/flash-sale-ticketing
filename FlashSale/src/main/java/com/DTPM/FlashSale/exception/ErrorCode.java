package com.DTPM.FlashSale.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // ===== Common 1xxx =====
    UNCATEGORIZED(1000, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_REQUEST(1001, "Invalid request", HttpStatus.BAD_REQUEST),
    VALIDATION_ERROR(1002, "Validation failed", HttpStatus.BAD_REQUEST),

    // ===== Auth 11xx =====
    UNAUTHORIZED(1100, "Unauthorized", HttpStatus.UNAUTHORIZED),
    INVALID_CREDENTIALS(1101, "Invalid email or password", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN(1102, "Invalid or expired token", HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED(1103, "You are not allowed to access this resource", HttpStatus.FORBIDDEN),

    // ===== User 12xx =====
    USER_NOT_FOUND(1200, "User not found", HttpStatus.NOT_FOUND),
    USER_ALREADY_EXISTS(1201, "Email already registered", HttpStatus.BAD_REQUEST),
    USER_BANNED(1202, "User is banned or inactive", HttpStatus.FORBIDDEN),
    CANNOT_BAN_SELF(1203, "Admin cannot ban themselves", HttpStatus.BAD_REQUEST),

    // ===== Event 13xx =====
    EVENT_NOT_FOUND(1300, "Event not found", HttpStatus.NOT_FOUND),
    EVENT_NOT_PUBLISHED(1301, "Event is not available for booking", HttpStatus.BAD_REQUEST),
    EVENT_NOT_MODIFIABLE(1302, "Event cannot be modified in current status", HttpStatus.BAD_REQUEST),
    EVENT_ACCESS_DENIED(1303, "You do not own this event", HttpStatus.FORBIDDEN),
    SESSION_NOT_FOUND(1310, "Show session not found", HttpStatus.NOT_FOUND),

    // ===== TicketType / Seat 14xx =====
    TICKET_TYPE_NOT_FOUND(1400, "Ticket type not found", HttpStatus.NOT_FOUND),
    TICKET_NOT_ON_SALE(1401, "Ticket is not on sale", HttpStatus.BAD_REQUEST),
    INVALID_TICKET_QUANTITY(1402, "Quantity is outside allowed min/max per order", HttpStatus.BAD_REQUEST),
    SEAT_LOCKED(1403, "Seat is currently locked by another user", HttpStatus.CONFLICT),
    SEAT_BOOKED(1404, "Seat is already booked", HttpStatus.CONFLICT),
    SEAT_NOT_FOUND(1405, "Seat not found", HttpStatus.NOT_FOUND),

    // ===== Order / Flash Sale 15xx =====
    ORDER_NOT_FOUND(1500, "Order not found", HttpStatus.NOT_FOUND),
    SOLD_OUT(1501, "Sold Out", HttpStatus.CONFLICT),
    ORDER_NOT_PENDING(1502, "Order is not in PENDING status", HttpStatus.BAD_REQUEST),
    ORDER_EXPIRED(1503, "Order hold time has expired", HttpStatus.BAD_REQUEST),
    ORDER_CANNOT_CANCEL(1504, "Order cannot be cancelled", HttpStatus.BAD_REQUEST),
    ORDER_CANNOT_PAY(1505, "Order cannot be paid", HttpStatus.BAD_REQUEST),
    ORDER_ACCESS_DENIED(1506, "You do not own this order", HttpStatus.FORBIDDEN),

    // ===== Payment 16xx =====
    PAYMENT_FAILED(1600, "Payment failed", HttpStatus.BAD_REQUEST),
    PAYMENT_ALREADY_EXISTS(1601, "Payment already exists for this order", HttpStatus.CONFLICT),

    // ===== Flash Sale Campaign 17xx =====
    CAMPAIGN_INACTIVE(1700, "Flash sale campaign is not active", HttpStatus.BAD_REQUEST),
    FLASH_SOLD_OUT(1701, "Flash sale campaign quota sold out", HttpStatus.CONFLICT);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}