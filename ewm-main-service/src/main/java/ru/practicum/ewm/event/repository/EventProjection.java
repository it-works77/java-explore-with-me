package ru.practicum.ewm.event.repository;

import ru.practicum.ewm.event.model.EventState;

import java.time.LocalDateTime;

public interface EventProjection {
    Long getId();

    String getTitle();

    String getAnnotation();

    String getDescription();

    Long getCategoryId();

    String getCategoryName();

    Long getInitiatorId();

    String getInitiatorName();

    LocalDateTime getEventDate();

    Boolean getPaid();

    Integer getParticipantLimit();

    Boolean getRequestModeration();

    EventState getState();

    LocalDateTime getCreatedOn();

    LocalDateTime getPublishedOn();
}
