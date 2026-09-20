package com.DTPM.FlashSale.exception;

import com.DTPM.FlashSale.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** AppException: ErrorCode → HTTP status đúng */
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<Void>> handleAppException(AppException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        ApiResponse<Void> response = ApiResponse.error(
                errorCode.getCode(),
                ex.getMessage() // custom message nếu có
        );
        return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
    }

    /** @Valid DTO */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .orElse(ErrorCode.VALIDATION_ERROR.getMessage());

        ApiResponse<Void> response = ApiResponse.error(
                ErrorCode.VALIDATION_ERROR.getCode(),
                message);
        return ResponseEntity.badRequest().body(response);
    }

    /** Fallback */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleOther(Exception ex) {
        log.error("Uncategorized error", ex);
        ApiResponse<Void> response = ApiResponse.error(
                ErrorCode.UNCATEGORIZED.getCode(),
                ErrorCode.UNCATEGORIZED.getMessage());
        return ResponseEntity.internalServerError().body(response);
    }
}