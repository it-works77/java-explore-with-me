package ru.practicum.ewm.event.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.category.dto.CategoryResponseDto;
import ru.practicum.ewm.user.dto.UserShortResponseDto;
import ru.practicum.stats.api.StatsApiConfig;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventShortResponseDto {
        private Long id;

        private String title;

        private String annotation;

        private CategoryResponseDto category;

        @JsonFormat(pattern = StatsApiConfig.DATE_TIME_PATTERN)
        private LocalDateTime eventDate;

        private UserShortResponseDto initiator;

        private boolean paid;

        private Long confirmedRequests;

        private Long views;
}
