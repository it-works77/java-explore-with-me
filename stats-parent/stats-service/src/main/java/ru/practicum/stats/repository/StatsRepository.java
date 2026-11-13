package ru.practicum.stats.repository;

import ru.practicum.stats.model.Stats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface StatsRepository extends JpaRepository<Stats, Long> {
    @Query("SELECT new ru.practicum.stats.repository.StatsWithHits(s.app, s.uri, COUNT(s.id)) " +
            "FROM Stats s WHERE s.createdAt " +
            "BETWEEN :start AND :end " +
            "GROUP BY s.app, s.uri " +
            "ORDER BY COUNT(s.id) DESC, s.app, s.uri")
    List<StatsWithHits> findAllBetween(@Param("start") LocalDateTime start,
                                       @Param("end") LocalDateTime end);

    @Query("SELECT new ru.practicum.stats.repository.StatsWithHits(s.app, s.uri, COUNT(DISTINCT s.ip)) " +
            "FROM Stats s WHERE s.createdAt " +
            "BETWEEN :start AND :end " +
            "GROUP BY s.app, s.uri " +
            "ORDER BY COUNT(DISTINCT s.ip) DESC, s.app, s.uri")
    List<StatsWithHits> findAllUniqueBetween(@Param("start") LocalDateTime start,
                                             @Param("end") LocalDateTime end);

    @Query("SELECT new ru.practicum.stats.repository.StatsWithHits(s.app, s.uri, COUNT(s.id)) " +
            "FROM Stats s WHERE s.createdAt " +
            "BETWEEN :start AND :end " +
            "AND s.uri IN (:uris) " +
            "GROUP BY s.app, s.uri " +
            "ORDER BY COUNT(s.id) DESC, s.app, s.uri")
    List<StatsWithHits> findAllByUrisBetween(@Param("start") LocalDateTime start,
                                            @Param("end") LocalDateTime end,
                                            @Param("uris") List<String> uris);

    @Query("SELECT new ru.practicum.stats.repository.StatsWithHits(s.app, s.uri, COUNT(DISTINCT s.ip)) " +
            "FROM Stats s WHERE s.createdAt " +
            "BETWEEN :start AND :end " +
            "AND s.uri IN (:uris) " +
            "GROUP BY s.app, s.uri " +
            "ORDER BY COUNT(DISTINCT s.ip) DESC, s.app, s.uri")
    List<StatsWithHits> findAllUniqueByUrisBetween(@Param("start") LocalDateTime start,
                                                  @Param("end") LocalDateTime end,
                                                  @Param("uris") List<String> uris);
}
