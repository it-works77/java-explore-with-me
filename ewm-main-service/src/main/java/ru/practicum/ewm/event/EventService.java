package ru.practicum.ewm.event;

import ru.practicum.ewm.event.dto.*;

import java.util.List;

public interface EventService {
    // User
    EventFullResponseDto add(Long userId, CreateEventRequestDto dto);

    // Получение полной информации о событии, добавленного текущим пользователем
    EventFullResponseDto get(Long userId, Long eventId);

    // Получение списка событий, добавленных текущим пользователем
    List<EventShortResponseDto> searchPublic(Long userId, int from, int size);

    // Изменение события добавленного текущим пользователем
    EventFullResponseDto update(Long userId, Long eventId, UpdateEventUserRequestDto dto);

    // Admin
    // Поиск событий
    List<EventFullResponseDto> searchAdmin(QueryEventAdminRequestDto dto);

    // Редактирование данных события и его статуса (отклонение/публикация)
    EventFullResponseDto updateByAdmin(Long eventId, UpdateEventAdminRequestDto dto);

    // Public
    // Получение списка событий с возможностью фильтрации
    List<EventShortResponseDto> searchPublic(QueryEventPublicRequestDto dto);

    // Получение подробной информации о событии
    EventFullResponseDto get(Long eventId);
}
