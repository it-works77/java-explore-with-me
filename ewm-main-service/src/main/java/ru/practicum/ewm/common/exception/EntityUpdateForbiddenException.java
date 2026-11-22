package ru.practicum.ewm.common.exception;

public class EntityUpdateForbiddenException extends RuntimeException {
    public EntityUpdateForbiddenException(String message) {
        super(message);
    }
}
