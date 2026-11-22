package ru.practicum.ewm.event.repository;

import jakarta.persistence.criteria.JoinType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import ru.practicum.ewm.event.dto.QueryEventAdminRequestDto;
import ru.practicum.ewm.event.dto.QueryEventPublicRequestDto;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventState;

import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EventSpecifications {
    /**
     * Подгружает category и initiator одним запросом (против N+1).
     * Пропускается для count-запроса (Spring Data JPA может вызывать count(spec)).
     */
    public static Specification<Event> fetchCategoryAndInitiator() {
        return (root, query, cb) -> {
            assert query != null;
            if (Long.class != query.getResultType()) {
                root.fetch("category", JoinType.LEFT);
                root.fetch("initiator", JoinType.LEFT);
            }
            return cb.conjunction();
        };
    }

    // ===== Общие фильтры (используются и админом, и пользователем) =====

    public static Specification<Event> byCategories(List<Long> categories) {
        return (root, query, cb) ->
                (categories == null || categories.isEmpty())
                        ? cb.conjunction()
                        : root.get("category").get("id").in(categories);
    }

    public static Specification<Event> byRangeStart(LocalDateTime rangeStart) {
        return (root, query, cb) ->
                (rangeStart == null)
                        ? cb.conjunction()
                        : cb.greaterThanOrEqualTo(root.get("eventDate"), rangeStart);
    }

    public static Specification<Event> byRangeEnd(LocalDateTime rangeEnd) {
        return (root, query, cb) ->
                (rangeEnd == null)
                        ? cb.conjunction()
                        : cb.lessThanOrEqualTo(root.get("eventDate"), rangeEnd);
    }

    // ===== Только для админа =====

    public static Specification<Event> byInitiators(List<Long> users) {
        return (root, query, cb) ->
                (users == null || users.isEmpty())
                        ? cb.conjunction()
                        : root.get("initiator").get("id").in(users);
    }

    public static Specification<Event> byStates(List<EventState> states) {
        return (root, query, cb) ->
                (states == null || states.isEmpty())
                        ? cb.conjunction()
                        : root.get("state").in(states);
    }

    // ===== Только для пользователя =====

    public static Specification<Event> byText(String text) {
        return (root, query, cb) ->
                (text == null || text.isBlank())
                        ? cb.conjunction()
                        : cb.or(
                        cb.like(cb.lower(root.get("annotation")), "%" + text.toLowerCase() + "%"),
                        cb.like(cb.lower(root.get("description")), "%" + text.toLowerCase() + "%")
                );
    }

    public static Specification<Event> byPaid(Boolean paid) {
        return (root, query, cb) ->
                (paid == null)
                        ? cb.conjunction()
                        : cb.equal(root.get("paid"), paid);
    }

    /**
     * onlyAvailable = true → только события, где confirmedRequests < participantLimit
     * (или participantLimit == 0, что означает «без ограничения»)
     */
    // TODO confirmedRequests вычисляемое, не реализовано. Сделать позже
    public static Specification<Event> byOnlyAvailable(Boolean onlyAvailable) {
        return (root, query, cb) ->
                (onlyAvailable == null || !onlyAvailable)
                        ? cb.conjunction()
                        : cb.or(
                        cb.equal(root.get("participantLimit"), 0),
                        cb.lessThan(root.get("confirmedRequests"), root.get("participantLimit"))
                );
    }

    // ===== Сборка для админа =====

    /**
     * Собирает все фильтры в одну спецификацию.
     * Пустые/отсутствующие фильтры дают cb.conjunction() — «WHERE 1=1», не влияет на запрос.
     */
    public static Specification<Event> combineForAdmin(QueryEventAdminRequestDto dto) {
        return Specification.where(fetchCategoryAndInitiator())
                .and(byInitiators(dto.getUsers()))
                .and(byStates(dto.getStates()))
                .and(byCategories(dto.getCategories()))
                .and(byRangeStart(dto.getRangeStart()))
                .and(byRangeEnd(dto.getRangeEnd()));
    }

    // ===== Сборка для пользователя =====

    /**
     * Собирает все фильтры в одну спецификацию.
     * Пустые/отсутствующие фильтры дают cb.conjunction() — «WHERE 1=1», не влияет на запрос.
     */
    public static Specification<Event> combineForUser(QueryEventPublicRequestDto dto) {
        return Specification.where(fetchCategoryAndInitiator())
                .and(byText(dto.getText()))
                .and(byCategories(dto.getCategories()))
                .and(byPaid(dto.getPaid()))
                .and(byRangeStart(dto.getRangeStart()))
                .and(byRangeEnd(dto.getRangeEnd()));
                // TODO confirmedRequests вычисляемое, не реализовано. Сделать позже
                // .and(byOnlyAvailable(dto.getOnlyAvailable()));

    }
}
