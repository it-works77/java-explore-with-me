package ru.practicum.stats;

import ru.practicum.stats.dto.GetStatsRequestDto;
import ru.practicum.stats.dto.SaveStatsRequestDto;
import ru.practicum.stats.dto.StatsResponseDto;

import java.util.List;

public interface StatsService {
    void save(SaveStatsRequestDto requestDto);

    List<StatsResponseDto> getStats(GetStatsRequestDto dto);
}
