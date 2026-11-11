package ru.practicum.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class GetStatsRequestDto {
    LocalDateTime start;
    LocalDateTime end;
    List<String> uris;
    boolean unique;
}
