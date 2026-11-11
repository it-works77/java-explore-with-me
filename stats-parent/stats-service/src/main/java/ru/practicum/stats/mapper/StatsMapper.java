package ru.practicum.stats.mapper;

import ru.practicum.stats.dto.SaveStatsRequestDto;
import ru.practicum.stats.dto.StatsResponseDto;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.practicum.stats.model.Stats;
import ru.practicum.stats.repository.StatsWithHits;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class StatsMapper {
    public static Stats mapRequestDtotoStats(SaveStatsRequestDto dto) {
        return Stats.builder()
                .app(dto.getApp())
                .uri(dto.getUri())
                .ip(dto.getIp())
                .createdAt(dto.getTimestamp())
                .build();
    }

    public static StatsResponseDto mapStatsToResponseDto(StatsWithHits stats) {
        return StatsResponseDto.builder()
                .app(stats.app())
                .uri(stats.uri())
                .hits(stats.hits())
                .build();
    }
}
