package ru.practicum.ewm.event;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.event.dto.EventShortResponseDto;
import ru.practicum.ewm.event.dto.QueryEventPublicRequestDto;
import ru.practicum.stats.api.StatsApiConfig;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventPublicController {
    private final EventService eventService;

    @GetMapping
    public List<EventShortResponseDto> search(@RequestParam(required = false) String text,
                                              @RequestParam(required = false) List<Long> categories,
                                              @RequestParam(required = false) boolean paid,
                                              @RequestParam(required = false)
                                              @DateTimeFormat(pattern = StatsApiConfig.DATE_TIME_PATTERN)
                                              LocalDateTime rangeStart,
                                              @RequestParam(required = false)
                                              @DateTimeFormat(pattern = StatsApiConfig.DATE_TIME_PATTERN)
                                              LocalDateTime rangeEnd,
                                              @RequestParam(defaultValue = "0") @PositiveOrZero int from,
                                              @RequestParam(defaultValue = "10") @Positive int size,
                                              HttpServletRequest request) {

        return eventService.searchPublic(QueryEventPublicRequestDto.builder()
                .text(text)
                .categories(categories)
                .paid(paid)
                .rangeStart(rangeStart)
                .rangeEnd(rangeEnd)
                .from(from)
                .size(size)
                .build(), request.getRequestURI(), request.getRemoteAddr());
    }

    @GetMapping("/events/{id}")
    public EventShortResponseDto get(@PathVariable @Positive Long id,
                                     HttpServletRequest request) {
        // событие должно быть опубликовано
        // информация о событии должна включать в себя количество просмотров и количество подтвержденных запросов
        // информацию о том, что по этому эндпоинту был осуществлен и обработан запрос, нужно сохранить в сервисе статистики

        return eventService.getPublishedEvent(id, request.getRequestURI(), request.getRemoteAddr());
    }

}
