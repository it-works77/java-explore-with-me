package ru.practicum.stats;

import ru.practictum.stats.dto.StatsRequestDto;
import ru.practictum.stats.dto.StatsResponseDto;

import java.time.LocalDateTime;
import java.util.List;

public interface StatsService {
    void save(StatsRequestDto requestDto);

    List<StatsResponseDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique);
}
