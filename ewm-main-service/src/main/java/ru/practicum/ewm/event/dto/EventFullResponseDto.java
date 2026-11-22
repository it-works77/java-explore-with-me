package ru.practicum.ewm.event.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.category.dto.CategoryResponseDto;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.user.dto.UserFullResponseDto;
import ru.practicum.ewm.user.dto.UserShortResponseDto;
import ru.practicum.stats.api.StatsApiConfig;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventFullResponseDto {
    private Long id;

    private String title;

    private String annotation;

    private String description;

    private CategoryResponseDto category;

    @JsonFormat(pattern = StatsApiConfig.DATE_TIME_PATTERN)
    private LocalDateTime eventDate;

    private UserShortResponseDto initiator;

    private LocationDto location;

    private boolean paid;

    private int participantLimit;

    private boolean requestModeration;

    private EventState state;

    private Long confirmedRequests;

    private Long views;

    @JsonFormat(pattern = StatsApiConfig.DATE_TIME_PATTERN)
    private LocalDateTime createdOn;

    @JsonFormat(pattern = StatsApiConfig.DATE_TIME_PATTERN)
    private LocalDateTime publishedOn;
}
