package ru.practicum.ewm.event.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.event.model.Event;

import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    @Query(value = "SELECT e.id as id, " +
            "   e.title as title, " +
            "   e.annotation as annotation, " +
            "   e.description as description, " +
            "   e.category_id as categoryId, " +
            "   c.name as categoryName, " +
            "   e.initiator_id as initiatorId, " +
            "   u.name as initiatorName, " +
            "   e.event_date as eventDate, " +
            "   e.paid as paid, " +
            "   e.participant_limit as participantLimit, " +
            "   e.request_moderation as requestModeration, " +
            "   e.state as state, " +
            "   e.created_on as createdOn, " +
            "   e.published_on as publishedOn " +
            "FROM events e " +
            "   LEFT JOIN categories c ON c.id = e.category_id " +
            "LEFT JOIN users u ON u.id = e.initiator_id " +
            "WHERE e.initiator_id = :userId " +
            "ORDER BY e.id DESC " +
            "LIMIT :size OFFSET :offset", nativeQuery = true)
    List<EventProjection> findAll(@Param("userId") Long userId,
                        @Param("offset") int offset,
                        @Param("size") int size);
}
