package ru.practicum.stats;

import org.springframework.transaction.annotation.Transactional;
import ru.practicum.stats.dto.GetStatsRequestDto;
import ru.practicum.stats.dto.SaveStatsRequestDto;
import ru.practicum.stats.dto.StatsResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.practicum.stats.mapper.StatsMapper;
import ru.practicum.stats.model.Stats;
import org.springframework.stereotype.Service;
import ru.practicum.stats.repository.StatsRepository;
import ru.practicum.stats.repository.StatsWithHits;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {
    private final StatsRepository statsRepository;

    @Override
    @Transactional
    public void save(SaveStatsRequestDto requestDto) {
        log.info("Saving stats: {}", requestDto);
        Stats stats = StatsMapper.mapRequestDtotoStats(requestDto);
        statsRepository.save(stats);
        log.info("Stats saved: {}", stats);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StatsResponseDto> getStats(GetStatsRequestDto dto) {
        log.info("Getting stats from {} to {} for uris {}", dto.getStart(), dto.getEnd(), dto.getUris());

        List<StatsWithHits> result;

        if (dto.isUnique()) {
            log.info("Getting unique stats");
            if (dto.getUris() == null) {
                result = statsRepository.findAllUniqueBetween(dto.getStart(), dto.getEnd());
            } else {
                result = statsRepository.findAllUniqueByUrisBetween(dto.getStart(), dto.getEnd(), dto.getUris());
            }
        } else {
            log.info("Getting non-unique stats");
            if (dto.getUris() == null) {
                result = statsRepository.findAllBetween(dto.getStart(), dto.getEnd());
            } else {
                result = statsRepository.findAllByUrisBetween(dto.getStart(), dto.getEnd(), dto.getUris());
            }
        }

        log.info("Result: {}", result);

        return result.stream()
                .map(StatsMapper::mapStatsToResponseDto)
                .toList();
    }
}
