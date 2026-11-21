package ru.practicum.ewm.common.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import ru.practicum.ewm.common.dto.ErrorResponse;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

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

    @ExceptionHandler(IllegalRequestParamException.class)
    public ResponseEntity<ErrorResponse> handleIllegalRequestParamException(IllegalRequestParamException ex) {
        log.warn("Неверный параметр запроса: {}", ex.getMessage());
        log.debug("Неверный параметр запроса", ex);
        return createErrorResponse(HttpStatus.BAD_REQUEST, "Неверный параметр запроса", ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.warn("Нарушение целостности: {}", ex.getMessage());
        log.debug("Нарушение целостности", ex);
        return createErrorResponse(HttpStatus.CONFLICT,
                "Нарушение целостности",
                "Нарушено ограничение целостности данных");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

        log.warn("Ошибка валидации: {}", errors);
        log.debug("Ошибка валидации", ex);

        return createErrorResponse(HttpStatus.BAD_REQUEST,
                "Ошибка валидации",
                "Ошибка валидации параметров запроса",
                errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {

        log.warn("Нарушение ограничений параметров: {}", ex.getMessage());
        log.debug("Нарушение ограничений параметров", ex);

        return createErrorResponse(HttpStatus.BAD_REQUEST,
                "Нарушение ограничений параметров",
                ex.getMessage());
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> handleIncorrectRequestErrors(Exception ex) {

        log.warn("Ошибка парсинга запроса: {}", ex.getMessage());
        log.debug("Ошибка парсинга запроса", ex);

        return createErrorResponse(HttpStatus.BAD_REQUEST,
                "Ошибка парсинга запроса",
                ex.getMessage());
    }

    @ExceptionHandler({HttpRequestMethodNotSupportedException.class})
    public ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupportedErrors(Exception ex) {

        log.warn("Метод не поддерживается: {}", ex.getMessage());
        log.debug("Метод не поддерживается", ex);

        return createErrorResponse(HttpStatus.METHOD_NOT_ALLOWED,
                "Метод не поддерживается",
                ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllErrors(Exception ex) {
        log.error("Внутренняя ошибка сервера: {}", ex.getMessage());
        log.debug("Внутренняя ошибка сервера:", ex);

        // TODO Hide internal details before release
        return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка сервера", ex.getMessage());
    }

    private ResponseEntity<ErrorResponse> createErrorResponse(HttpStatus status,
                                                              String reason,
                                                              String message) {
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

    private ResponseEntity<ErrorResponse> createErrorResponse(HttpStatus status,
                                                              String reason,
                                                              String message,
                                                              Map<String, String> errors) {
        return ResponseEntity
                .status(status)
                .body(ErrorResponse.builder()
                        .status(status.name())
                        .reason(reason)
                        .message(message)
                        .errors(errors)
                        .timestamp(LocalDateTime.now(ZoneOffset.UTC))
                        .build()
                );
    }
}
