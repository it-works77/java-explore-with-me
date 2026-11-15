package ru.practicum.stats.exception;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class InvalidStatsPeriodException extends RuntimeException {
    private final LocalDateTime start;
    private final LocalDateTime end;

    public InvalidStatsPeriodException(LocalDateTime start, LocalDateTime end) {
        super("Дата начала (" + start + ") не может быть позже даты окончания (" + end + ")");
        this.start = start;
        this.end = end;
    }
}
