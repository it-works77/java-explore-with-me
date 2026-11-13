package ru.practicum.stats.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class ValidationErrorResponse {
    private String error;
    private Map<String, String> errors;
}
