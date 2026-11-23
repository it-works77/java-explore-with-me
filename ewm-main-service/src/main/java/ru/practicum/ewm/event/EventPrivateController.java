package ru.practicum.ewm.event;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.ewm.event.dto.CreateEventRequestDto;
import ru.practicum.ewm.event.dto.EventFullResponseDto;
import ru.practicum.ewm.event.dto.EventShortResponseDto;
import ru.practicum.ewm.event.dto.UpdateEventUserRequestDto;

import java.util.List;

@RestController
@Validated
@RequestMapping("/users")
@RequiredArgsConstructor
public class EventPrivateController {
    private final EventService eventService;

    @PostMapping("/{userId}/events")
    @ResponseStatus(HttpStatus.CREATED)
    public EventFullResponseDto add(@PathVariable @Positive Long userId,
                                    @Valid @RequestBody CreateEventRequestDto dto) {
        return eventService.add(userId, dto);
    }

    @GetMapping("/{userId}/events")
    public List<EventShortResponseDto> getAll(@PathVariable @Positive Long userId,
                                              @RequestParam(defaultValue = "0") @PositiveOrZero int from,
                                              @RequestParam(defaultValue = "10") @Positive int size) {
        return eventService.searchPublic(userId, from, size);
    }

    @GetMapping("/{userId}/events/{eventId}")
    public EventFullResponseDto get(@PathVariable @Positive Long userId,
                                    @PathVariable @Positive Long eventId) {
        return eventService.get(userId, eventId);
    }

    @PatchMapping("/{userId}/events/{eventId}")
    public EventFullResponseDto update(@PathVariable @Positive Long userId,
                                       @PathVariable @Positive Long eventId,
                                       @Valid @RequestBody UpdateEventUserRequestDto dto) {
        return eventService.update(userId, eventId, dto);
    }
}
