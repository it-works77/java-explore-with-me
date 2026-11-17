package ru.practicum.ewm.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import ru.practicum.ewm.common.dto.ErrorResponse;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFoundExceptionErrors(EntityNotFoundException ex) {
        log.warn("Объект не найден: {}", ex.getMessage());
        log.debug("Объект не найден", ex);
        return createErrorResponse(HttpStatus.NOT_FOUND, "Объект не найден", ex.getMessage());
    }

    @ExceptionHandler(EntityAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEntityAlreadyExistsErrors(EntityAlreadyExistsException ex) {
        log.warn("Нарушение уникальности: {}", ex.getMessage());
        log.debug("Нарушение уникальности", ex);
        return createErrorResponse(HttpStatus.CONFLICT, "Нарушение уникальности", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllErrors(Exception ex) {
        log.error("Внутренняя ошибка сервера: {}", ex.getMessage());
        log.debug("Внутренняя ошибка сервера:", ex);

        // TODO Hide internal details before release
        return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка сервера", ex.getMessage());
    }

    private ResponseEntity<ErrorResponse> createErrorResponse(HttpStatus status, String reason, String message) {
        return ResponseEntity
                .status(status)
                .body(ErrorResponse.builder()
                        .status(status.name())
                        .reason(reason)
                        .message(message)
                        .timestamp(LocalDateTime.now(ZoneOffset.UTC))
                        .build()
                );
    }
}
