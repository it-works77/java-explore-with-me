package ru.practicum.stats.exception;

import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import ru.practicum.stats.dto.ErrorResponse;
import ru.practicum.stats.dto.ValidationErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> handleIncorrectRequestErrors(Exception ex) {
        ErrorResponse body = ErrorResponse.builder()
                .error("Ошибка парсинга запроса")
                .details(ex.getMessage())
                .build();
        log.warn("Ошибка парсинга запроса: {}", ex.getMessage());
        log.debug("Ошибка парсинга запроса", ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

        ValidationErrorResponse body = ValidationErrorResponse.builder()
                .error("Ошибка валидации")
                .errors(errors)
                .build();
        log.warn("Ошибка валидации: {}", errors);
        log.debug("Ошибка валидации", ex);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(InvalidStatsPeriodException.class)
    public ResponseEntity<ValidationErrorResponse> handleInvalidPeriod(InvalidStatsPeriodException ex) {
        ValidationErrorResponse body = ValidationErrorResponse.builder()
                .error("Ошибка валидации")
                .errors(Map.of("start", ex.getMessage()))
                .build();

        log.warn("Некорректный период запроса: start={}, end={}", ex.getStart(), ex.getEnd());
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestParam(MissingServletRequestParameterException ex) {
        ErrorResponse body = ErrorResponse.builder()
                .error("Отсутствует обязательный параметр")
                .details(ex.getMessage())
                .build();
        log.warn("Отсутствует обязательный параметр: {}", ex.getMessage());
        log.debug("Отсутствует обязательный параметр", ex);
        return ResponseEntity.badRequest().body(body);
    }

    // Обработка 405 Method Not Allowed
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        ErrorResponse body = ErrorResponse.builder()
                .error("Неверный метод запроса")
                .details(ex.getMessage())
                .build();
        log.warn("Метод не поддерживается: {}", ex.getMethod());
        log.debug("Метод не поддерживается", ex);
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(body);
    }

    // Обработка 404 Not Found
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException ex) {
        ErrorResponse body = ErrorResponse.builder()
                .error("Неверный путь запроса")
                .details(ex.getResourcePath())
                .build();
        log.warn("Неверный путь запроса: {}", ex.getResourcePath());
        log.debug("Неверный путь запроса", ex);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllErrors(Exception ex) {
        ErrorResponse body = ErrorResponse.builder()
                .error("Ошибка сервера")
                .details("Внутренняя ошибка сервера")
                .build();
        log.error("Внутренняя ошибка сервера: {}", ex.getMessage());
        log.debug("Внутренняя ошибка сервера:", ex);
        return ResponseEntity.internalServerError().body(body);
    }
}
