package ru.practicum.ewm.event;

import jakarta.validation.constraints.Positive;
import ru.practicum.ewm.event.dto.*;

import java.util.List;

public interface EventService {
    // User
    EventFullResponseDto add(Long userId, CreateEventRequestDto dto);

    // Получение полной информации о событии, добавленного текущим пользователем
    EventFullResponseDto get(Long userId, Long eventId);

    // Получение списка событий, добавленных текущим пользователем
    List<EventShortResponseDto> getAllWithPagination(Long userId, int from, int size);

    // Изменение события добавленного текущим пользователем
    EventFullResponseDto update(Long userId, Long eventId, UpdateEventUserRequestDto dto);

    // Admin
    // Поиск событий
    List<EventFullResponseDto> searchAdmin(QueryEventAdminRequestDto dto);

    // Редактирование данных события и его статуса (отклонение/публикация)
    EventFullResponseDto updateByAdmin(Long eventId, UpdateEventAdminRequestDto dto);

    // Public
    // Получение списка событий с возможностью фильтрации
    List<EventShortResponseDto> searchPublic(QueryEventPublicRequestDto dto, String requestURI, String remoteAddr);

    EventFullResponseDto getPublishedEvent(@Positive Long id, String requestURI, String remoteAddr);
}
