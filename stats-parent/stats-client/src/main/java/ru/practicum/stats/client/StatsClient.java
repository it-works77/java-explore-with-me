package ru.practicum.stats.client;

import ru.practicum.stats.dto.SaveStatsRequestDto;
import ru.practicum.stats.dto.GetStatsRequestDto;
import ru.practicum.stats.dto.StatsResponseDto;

import java.util.List;

public interface StatsClient {

    void saveHit(SaveStatsRequestDto saveStatsRequestDto);

    List<StatsResponseDto> getStats(GetStatsRequestDto getStatsRequestDto);
}
