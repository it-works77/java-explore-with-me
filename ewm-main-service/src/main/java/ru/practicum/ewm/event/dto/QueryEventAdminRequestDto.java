package ru.practicum.ewm.event.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.stats.api.StatsApiConfig;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryEventAdminRequestDto {
    private List<@Positive Long> users;

    private List<EventState> states;

    private List<@Positive Long> categories;

    @JsonFormat(pattern = StatsApiConfig.DATE_TIME_PATTERN)
    private LocalDateTime rangeStart;

    @JsonFormat(pattern = StatsApiConfig.DATE_TIME_PATTERN)
    private LocalDateTime rangeEnd;

    @PositiveOrZero
    @Builder.Default
    private int from = 0;

    @Positive
    @Builder.Default
    private int size = 10;
}
