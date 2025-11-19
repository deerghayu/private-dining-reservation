package com.opentable.reservation.config;

import com.opentable.reservation.exception.BusinessException;
import com.opentable.reservation.exception.NotFoundException;
import com.opentable.reservation.exception.RoomAlreadyBookedException;
import jakarta.persistence.OptimisticLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RoomAlreadyBookedException.class)
    public ResponseEntity<Map<String, Object>> handleRoomAlreadyBookedException(RoomAlreadyBookedException exception) {
        return build(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessException(BusinessException exception) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFoundException(NotFoundException exception) {
        return build(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationError(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("Validation error");
        return build(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler({OptimisticLockException.class, ObjectOptimisticLockingFailureException.class})
    public ResponseEntity<Map<String, Object>> handleOptimisticLockException(Exception exception) {
        return build(HttpStatus.CONFLICT,
                "The reservation was modified by another user. Please refresh and try again.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(DataIntegrityViolationException exception) {
        // Database unique constraint violation (e.g., double-booking attempt)
        String message = exception.getMessage();

        // Check for room/date/slot constraint violation
        if (message != null && (message.toUpperCase().contains("UK_ROOM_DATE_SLOT")
                || (message.contains("ROOM_ID") && message.contains("RESERVATION_DATE") && message.contains("TIME_SLOT")))) {
            return build(HttpStatus.CONFLICT,
                    "This room and time slot is already booked. Please select a different time.");
        }

        return build(HttpStatus.CONFLICT, "A database constraint was violated. Please check your request.");
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String message) {
        Map<String, Object> body = Map.of(
                "code", status.value(),
                "message", message
        );
        return ResponseEntity.status(status).body(body);
    }
}