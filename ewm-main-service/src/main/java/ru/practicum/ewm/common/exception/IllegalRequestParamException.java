package ru.practicum.ewm.common.exception;

public class IllegalRequestParamException extends RuntimeException {
    public IllegalRequestParamException(String message) {
        super(message);
    }
}
