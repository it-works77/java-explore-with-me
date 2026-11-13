package ru.practicum.stats;

import ru.practicum.stats.api.StatsApiConfig;
import ru.practicum.stats.dto.GetStatsRequestDto;
import ru.practicum.stats.dto.SaveStatsRequestDto;
import ru.practicum.stats.dto.StatsResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class StatsController {
    private final StatsService statsService;

    @PostMapping("/hit")
    @ResponseStatus(HttpStatus.CREATED)
    public void save(@Valid @RequestBody SaveStatsRequestDto requestDto) {
        statsService.save(requestDto);
    }

    @GetMapping("/stats")
    public List<StatsResponseDto> getStats(@RequestParam
                                           @DateTimeFormat(pattern = StatsApiConfig.DATE_TIME_PATTERN) LocalDateTime start,
                                           @RequestParam
                                           @DateTimeFormat(pattern = StatsApiConfig.DATE_TIME_PATTERN) LocalDateTime end,
                                           @RequestParam(required = false) List<String> uris,
                                           @RequestParam(defaultValue = "false") boolean unique) {
        return statsService.getStats(new GetStatsRequestDto(start, end, uris, unique));
    }
}
