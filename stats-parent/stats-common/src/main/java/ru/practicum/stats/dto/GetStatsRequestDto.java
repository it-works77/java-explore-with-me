package ru.practicum.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class GetStatsRequestDto {
    private LocalDateTime start;
    private LocalDateTime end;
    private List<String> uris;
    private boolean unique;
}
