package ru.practicum.ewm.event;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.ewm.event.dto.EventFullResponseDto;
import ru.practicum.ewm.event.dto.QueryEventAdminRequestDto;
import ru.practicum.ewm.event.dto.UpdateEventAdminRequestDto;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.stats.api.StatsApiConfig;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@Validated
@RequestMapping("/admin/events")
@RequiredArgsConstructor
public class EventAdminController {
    private final EventService eventService;

    @GetMapping
    public List<EventFullResponseDto> search(@RequestParam(required = false) List<Long> users,
                                             @RequestParam(required = false) List<EventState> states,
                                             @RequestParam(required = false) List<Long> categories,
                                             @RequestParam(required = false)
                                             @DateTimeFormat(pattern = StatsApiConfig.DATE_TIME_PATTERN)
                                             LocalDateTime rangeStart,
                                             @RequestParam(required = false)
                                             @DateTimeFormat(pattern = StatsApiConfig.DATE_TIME_PATTERN)
                                             LocalDateTime rangeEnd,
                                             @RequestParam(defaultValue = "0") @PositiveOrZero int from,
                                             @RequestParam(defaultValue = "10") @Positive int size) {
        QueryEventAdminRequestDto query = QueryEventAdminRequestDto.builder()
                .users(users)
                .states(states)
                .categories(categories)
                .rangeStart(rangeStart)
                .rangeEnd(rangeEnd)
                .from(from)
                .size(size)
                .build();
        return eventService.searchAdmin(query);
    }

    @PatchMapping("/{eventId}")
    public EventFullResponseDto update(@PathVariable @Positive Long eventId,
                                       @RequestBody UpdateEventAdminRequestDto dto) {
        return eventService.updateByAdmin(eventId, dto);
    }
}
