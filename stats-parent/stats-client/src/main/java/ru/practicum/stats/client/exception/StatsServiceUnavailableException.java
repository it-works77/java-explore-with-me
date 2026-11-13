package ru.practicum.stats.client.exception;

public class StatsServiceUnavailableException extends StatsClientException {
    public StatsServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
