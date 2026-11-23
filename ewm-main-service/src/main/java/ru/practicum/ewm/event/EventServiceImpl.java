package ru.practicum.ewm.event;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.category.repository.CategoryRepository;
import ru.practicum.ewm.common.exception.EntityNotFoundException;
import ru.practicum.ewm.common.exception.EntityUpdateForbiddenException;
import ru.practicum.ewm.event.dto.*;
import ru.practicum.ewm.event.mapper.EventMapper;
import ru.practicum.ewm.event.model.AdminStateAction;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventSortType;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.event.model.UserStateAction;
import ru.practicum.ewm.event.repository.EventProjection;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.event.repository.EventSpecifications;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {
    private static final int MIN_HOURS_BEFORE_EVENT_DATE = 2;
    private static final int MIN_ADMIN_HOURS_BEFORE_EVENT_DATE = 1;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    @PersistenceContext
    private EntityManager entityManager;

    // User
    @Override
    @Transactional
    public EventFullResponseDto add(Long userId, CreateEventRequestDto dto) {
        log.info("Add event {} for user id={}", dto, userId);
        User user = userRepository.findById(userId).orElseThrow(
                () -> new EntityNotFoundException("Пользователь не найден по id=%s".formatted(userId)));
        Category category = categoryRepository.findById(dto.getCategory()).orElseThrow(
                () -> new EntityNotFoundException("Категория не найдена по id=%s".formatted(dto.getCategory())));

        // дата и время на которые намечено событие не может быть раньше, чем через два часа от текущего момента
        if (dto.getEventDate().isBefore(LocalDateTime.now().plusHours(MIN_HOURS_BEFORE_EVENT_DATE))) {
            throw new EntityUpdateForbiddenException("Дата и время на которые намечено событие " +
                    "не может быть раньше, чем через два часа от текущего момента");
        }

        Event event = EventMapper.mapCreateEventRequestDtoToEvent(dto, category, user);
        Event saved = eventRepository.save(event);
        log.debug("Event saved: {}", saved);
        return EventMapper.mapEventToEventFullResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public EventFullResponseDto get(Long userId, Long eventId) {
        log.info("Get event id={} for user id={}", eventId, userId);

        Event event = eventRepository.findById(eventId).orElseThrow(
                () -> new EntityNotFoundException("Событие не найдено по id=%s".formatted(eventId)));
        if (!event.getInitiator().getId().equals(userId)) {
            throw new EntityNotFoundException("У пользователя id=%s нет события id=%s".formatted(userId, eventId));
        }

        log.debug("Event for user id={} found: {}", userId, event);
        return EventMapper.mapEventToEventFullResponseDto(event);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventShortResponseDto> searchPublic(Long userId, int offset, int size) {
        log.info("Get all events for user id={} with pagination: offset={}, size={}", userId, offset, size);
        List<EventProjection> eventProjections = eventRepository.findAll(userId, offset, size);
        return eventProjections.stream()
                .map(EventMapper::mapEventProjectionToEventShortResponseDto)
                .toList();
    }

    @Override
    @Transactional
    public EventFullResponseDto update(Long userId, Long eventId, UpdateEventUserRequestDto dto) {
        log.info("Update event id={} for user id={}", eventId, userId);
        Event existingEvent = eventRepository.findById(eventId).orElseThrow(
                () -> new EntityNotFoundException("Событие не найдено по id=%s".formatted(eventId)));
        if (!existingEvent.getInitiator().getId().equals(userId)) {
            throw new EntityNotFoundException("У пользователя id=%s нет события id=%s".formatted(userId, eventId));
        }

        // изменить можно только отмененные события или события в состоянии ожидания модерации
        // (Ожидается код ошибки 409)
        if (existingEvent.getState() != EventState.CANCELED && existingEvent.getState() != EventState.PENDING) {
            throw new EntityUpdateForbiddenException("Событие не может быть изменено: " +
                    "можно только отмененные события или события в состоянии ожидания модерации");
        }
        // дата и время на которые намечено событие не может быть раньше,
        // чем через два часа от текущего момента (Ожидается код ошибки 409)
        if (existingEvent.getEventDate().isBefore(LocalDateTime.now().plusHours(MIN_HOURS_BEFORE_EVENT_DATE))) {
            throw new EntityUpdateForbiddenException("Событие не может быть изменено: " +
                    "дата и время на которые намечено событие не может быть раньше, " +
                    "чем через %S час(а) от текущего момента".formatted(MIN_HOURS_BEFORE_EVENT_DATE));
        }

        Event updatedEvent = updateEventByUser(existingEvent, dto);
        if (updatedEvent.getEventDate().isBefore(LocalDateTime.now().plusHours(MIN_HOURS_BEFORE_EVENT_DATE))) {
            throw new EntityUpdateForbiddenException("Событие не может быть изменено: " +
                    "дата и время на которые намечено событие не может быть раньше, " +
                    "чем через %s час(а) от текущего момента".formatted(MIN_HOURS_BEFORE_EVENT_DATE));
        }
        Event saved = eventRepository.save(updatedEvent);
        log.debug("Event updated: {}", saved);
        return EventMapper.mapEventToEventFullResponseDto(saved);
    }

    // Admin
    @Override
    @Transactional(readOnly = true)
    public List<EventFullResponseDto> searchAdmin(QueryEventAdminRequestDto dto) {
        log.info("Search events by admin");
        Specification<Event> spec = EventSpecifications.combineForAdmin(dto);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Event> cq = cb.createQuery(Event.class);
        Root<Event> root = cq.from(Event.class);

        // Применяем спецификацию (WHERE)
        Predicate predicate = spec.toPredicate(root, cq, cb);
        if (predicate != null) {
            cq.where(predicate);
        }

        // Сортировка по id DESC
        cq.orderBy(cb.desc(root.get("id")));

        // Произвольный offset/size через setFirstResult/setMaxResults
        int offset = dto.getFrom();
        int size = dto.getSize();

        List<Event> events = entityManager.createQuery(cq)
                .setFirstResult(offset)
                .setMaxResults(size)
                .getResultList();

        return events.stream()
                .map(EventMapper::mapEventToEventFullResponseDto)
                .toList();
    }

    @Override
    @Transactional
    public EventFullResponseDto updateByAdmin(Long eventId, UpdateEventAdminRequestDto dto) {
        log.info("Update event id={} by admin", eventId);
        Event existingEvent = eventRepository.findById(eventId).orElseThrow(
                () -> new EntityNotFoundException("Событие не найдено по id=%s".formatted(eventId)));

        Event updatedEvent = updateEventByAdmin(existingEvent, dto);

        log.debug("Event updated by admin: {}", updatedEvent);
        return EventMapper.mapEventToEventFullResponseDto(updatedEvent);
    }

    // Public
    @Override
    @Transactional(readOnly = true)
    public List<EventShortResponseDto> searchPublic(QueryEventPublicRequestDto dto) {
        log.info("Get all events");

        Specification<Event> spec = EventSpecifications.combineForUser(dto)
                .and(EventSpecifications.byState(EventState.PUBLISHED));
        if (dto.getRangeStart() == null && dto.getRangeEnd() == null) {
            spec = spec.and(EventSpecifications.byRangeStart(LocalDateTime.now()));
        }

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Event> cq = cb.createQuery(Event.class);
        Root<Event> root = cq.from(Event.class);

        // Применяем спецификацию (WHERE)
        Predicate predicate = spec.toPredicate(root, cq, cb);
        if (predicate != null) {
            cq.where(predicate);
        }
        // === Сортировка на основе dto.getSort() ===
        EventSortType sort = dto.getSort() == null ? EventSortType.EVENT_DATE : dto.getSort();
        switch (sort) {
            // views приходят из stats-service и в сущности отсутствуют: до подключения сортируем по дате
            case VIEWS, EVENT_DATE -> cq.orderBy(cb.asc(root.get("eventDate")));
        }

        // Произвольный offset/size через setFirstResult/setMaxResults
        int offset = dto.getFrom();
        int size = dto.getSize();

        List<Event> events = entityManager.createQuery(cq)
                .setFirstResult(offset)
                .setMaxResults(size)
                .getResultList();

        return events.stream()
                .map(EventMapper::mapEventToEventShortResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EventFullResponseDto get(Long eventId) {
        log.info("Get event id={}", eventId);
        Event event = eventRepository.findById(eventId).orElseThrow(
                () -> new EntityNotFoundException("Событие не найдено по id=%s".formatted(eventId)));
        if (event.getState() != EventState.PUBLISHED) {
            throw new EntityNotFoundException("Событие не найдено по id=%s".formatted(eventId));
        }

        log.debug("Event found: {}", event);
        return EventMapper.mapEventToEventFullResponseDto(event);
    }

    private Event updateEventByUser(Event event, UpdateEventUserRequestDto dto) {
        if (dto.getTitle() != null) {
            event.setTitle(dto.getTitle());
        }
        if (dto.getAnnotation() != null) {
            event.setAnnotation(dto.getAnnotation());
        }
        if (dto.getDescription() != null) {
            event.setDescription(dto.getDescription());
        }
        if (dto.getCategory() != null) {
            Category category = categoryRepository.findById(dto.getCategory()).orElseThrow(
                    () -> new EntityNotFoundException("Категория не найдена по id=%s".formatted(dto.getCategory())));
            event.setCategory(category);
        }
        if (dto.getEventDate() != null) {
            event.setEventDate(dto.getEventDate());
        }

        if (dto.getLocation() != null) {
            event.setLatitude(dto.getLocation().lat());
            event.setLongitude(dto.getLocation().lon());
        }

        if (dto.getPaid() != null) {
            event.setPaid(dto.getPaid());
        }
        if (dto.getParticipantLimit() != null) {
            event.setParticipantLimit(dto.getParticipantLimit());
        }
        if (dto.getRequestModeration() != null) {
            event.setRequestModeration(dto.getRequestModeration());
        }

        if (dto.getStateAction() == UserStateAction.SEND_TO_REVIEW) {
            event.setState(EventState.PENDING);
        }
        if (dto.getStateAction() == UserStateAction.CANCEL_REVIEW) {
            event.setState(EventState.CANCELED);
        }

        return event;
    }

    private Event updateEventByAdmin(Event event, UpdateEventAdminRequestDto dto) {
        if (dto.getTitle() != null) {
            event.setTitle(dto.getTitle());
        }
        if (dto.getAnnotation() != null) {
            event.setAnnotation(dto.getAnnotation());
        }
        if (dto.getDescription() != null) {
            event.setDescription(dto.getDescription());
        }
        if (dto.getCategory() != null) {
            Category category = categoryRepository.findById(dto.getCategory()).orElseThrow(
                    () -> new EntityNotFoundException("Категория не найдена по id=%s".formatted(dto.getCategory())));
            event.setCategory(category);
        }
        if (dto.getEventDate() != null) {
            event.setEventDate(dto.getEventDate());
        }

        if (dto.getLocation() != null) {
            event.setLatitude(dto.getLocation().lat());
            event.setLongitude(dto.getLocation().lon());
        }

        if (dto.getPaid() != null) {
            event.setPaid(dto.getPaid());
        }
        if (dto.getParticipantLimit() != null) {
            event.setParticipantLimit(dto.getParticipantLimit());
        }
        if (dto.getRequestModeration() != null) {
            event.setRequestModeration(dto.getRequestModeration());
        }

        // stateAction processing
        if (dto.getStateAction() != null) {
            // событие можно публиковать, только если оно в состоянии ожидания публикации (Ожидается код ошибки 409)
            if (dto.getStateAction() == AdminStateAction.PUBLISH_EVENT) {
                if (event.getState() != EventState.PENDING) {
                    throw new EntityUpdateForbiddenException("Событие не может быть опубликовано: " +
                            "можно опубликовать только события в состоянии ожидания публикации");
                }
                LocalDateTime publishedOn = LocalDateTime.now();
                // «дата начала события не ранее чем за час от даты публикации»
                if (event.getEventDate().isBefore(publishedOn.plusHours(MIN_ADMIN_HOURS_BEFORE_EVENT_DATE))) {
                    throw new EntityUpdateForbiddenException("Событие не может быть опубликовано: " +
                            "дата начала события не может быть раньше, чем через %s час(а) от даты публикации"
                                    .formatted(MIN_ADMIN_HOURS_BEFORE_EVENT_DATE));
                }
                event.setState(EventState.PUBLISHED);
                event.setPublishedOn(publishedOn);
            }
            // событие можно отклонить, только если оно еще не опубликовано (Ожидается код ошибки 409)
            if (dto.getStateAction() == AdminStateAction.REJECT_EVENT) {
                if (event.getState() == EventState.PUBLISHED) {
                    throw new EntityUpdateForbiddenException("Событие не может быть отклонено: " +
                            "можно отклонить только события, которые еще не опубликованы");
                }
                event.setState(EventState.CANCELED);
            }
        }
        return event;
    }
}
