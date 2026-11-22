package ru.practicum.ewm.event.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.event.model.AdminStateAction;
import ru.practicum.ewm.event.model.UserStateAction;
import ru.practicum.stats.api.StatsApiConfig;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEventAdminRequestDto {
    private String title;

    private String annotation;

    private String description;

    private Long category;

    @JsonFormat(pattern = StatsApiConfig.DATE_TIME_PATTERN)
    private LocalDateTime eventDate;

    private LocationDto location;

    private boolean paid;

    @Builder.Default
    private int participantLimit = 0;

    @Builder.Default
    private boolean requestModeration = true;

    private AdminStateAction stateAction;
}
