package ru.practicum.stats;

import ru.practictum.stats.dto.StatsRequestDto;
import ru.practictum.stats.dto.StatsResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.practicum.stats.mapper.StatsMapper;
import ru.practicum.stats.model.Stats;
import org.springframework.stereotype.Service;
import ru.practicum.stats.repository.StatsRepository;
import ru.practicum.stats.repository.StatsWithHits;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {
    private final StatsRepository statsRepository;

    @Override
    public void save(StatsRequestDto requestDto) {
        log.info("Saving stats: {}", requestDto);
        Stats stats = StatsMapper.mapRequestDtotoStats(requestDto);
        statsRepository.save(stats);
        log.info("Stats saved: {}", stats);
    }

    @Override
    public List<StatsResponseDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique) {
        log.info("Getting stats from {} to {} for uris {}", start, end, uris);

        List<StatsWithHits> result;

        if (unique) {
            log.info("Getting unique stats");
            if (uris == null) {
                result = statsRepository.findAllUniqueBetween(start, end);
            } else {
                result = statsRepository.findAllUniqueByUrisBetween(start, end, uris);
            }
        } else {
            log.info("Getting non-unique stats");
            if (uris == null) {
                result = statsRepository.findAllBetween(start, end);
            } else {
                result = statsRepository.findAllByUrisBetween(start, end, uris);
            }
        }

        log.info("Result: {}", result);

        return result.stream()
                .map(StatsMapper::mapStatsToResponseDto)
                .toList();
    }
}
