package ru.practicum.ewm.event.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.category.mapper.CategoryMapper;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.event.dto.CreateEventRequestDto;
import ru.practicum.ewm.event.dto.EventFullResponseDto;
import ru.practicum.ewm.event.dto.LocationDto;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.user.mapper.UserMapper;
import ru.practicum.ewm.user.model.User;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EventMapper {
    public static Event mapCreateEventRequestDtoToEvent(CreateEventRequestDto dto,
                                                        Category category, User user) {
        return Event.builder()
                .title(dto.getTitle())
                .annotation(dto.getAnnotation())
                .description(dto.getDescription())
                .category(category)
                .initiator(user)
                .eventDate(dto.getEventDate())
                .latitude(dto.getLocation().lat())
                .longitude(dto.getLocation().lon())
                .paid(dto.isPaid())
                .participantLimit(dto.getParticipantLimit())
                .build();
    }

    public static EventFullResponseDto mapEventToEventFullResponseDto(Event event) {
        return EventFullResponseDto.builder()
                .id(event.getId())
                .title(event.getTitle())
                .annotation(event.getAnnotation())
                .description(event.getDescription())
                .category(CategoryMapper.mapCategoryToCategoryResponseDto(event.getCategory()))
                .initiator(UserMapper.mapUserToUserShortDto(event.getInitiator()))
                .eventDate(event.getEventDate())
                .location(new LocationDto(event.getLatitude(), event.getLongitude()))
                .paid(event.isPaid())
                .participantLimit(event.getParticipantLimit())
                .build();
    }
}
