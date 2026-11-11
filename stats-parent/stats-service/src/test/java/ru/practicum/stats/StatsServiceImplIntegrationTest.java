package ru.practicum.stats;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.stats.dto.StatsRequestDto;
import ru.practicum.stats.dto.StatsResponseDto;
import ru.practicum.stats.repository.StatsRepository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class StatsServiceImplIntegrationTest {
    private static final LocalDateTime BASE_TIME = LocalDateTime.of(2024, 9, 6, 11, 0, 0);
    private static final LocalDateTime WINDOW_START = BASE_TIME.minusHours(1);
    private static final LocalDateTime WINDOW_END = BASE_TIME.plusHours(1);

    private static final String APP_EWM = "ewm-main-service";
    private static final String APP_STATS = "stats-service";
    private static final String URI_ONE = "/events/1";
    private static final String URI_TWO = "/events/2";
    private static final String URI_TEN = "/events/10";
    private static final String IP_ONE = "192.163.0.1";
    private static final String IP_TWO = "192.163.0.2";
    private static final String IP_THREE = "192.163.0.3";

    private final StatsService statsService;
    private final StatsRepository statsRepository;

    @BeforeEach
    void clearStats() {
        statsRepository.deleteAllInBatch();
    }

    // ---------- сохранение и агрегация ----------

    @Test
    void saveOne_getSaved() {
        statsService.save(hit(APP_EWM, URI_ONE, IP_ONE, BASE_TIME));

        List<StatsResponseDto> responseDtos = statsService.getStats(WINDOW_START, WINDOW_END, null, false);

        assertEquals(1, responseDtos.size());

        StatsResponseDto responseDto = responseDtos.getFirst();
        assertEquals(APP_EWM, responseDto.getApp());
        assertEquals(URI_ONE, responseDto.getUri());
        assertEquals(1L, responseDto.getHits());
    }

    @Test
    void saveTwo_whenSecondHasTwoHits_getSaved() {
        save(
                hit(APP_EWM, URI_ONE, IP_ONE, BASE_TIME),
                hit(APP_STATS, URI_TWO, IP_ONE, BASE_TIME.plusMinutes(1)),
                hit(APP_STATS, URI_TWO, IP_TWO, BASE_TIME.plusMinutes(1))
        );

        List<StatsResponseDto> responseDtos = statsService.getStats(WINDOW_START, WINDOW_END, null, false);

        assertEquals(2, responseDtos.size());

        StatsResponseDto responseDto1 = responseDtos.getFirst();
        assertEquals(APP_EWM, responseDto1.getApp());
        assertEquals(URI_ONE, responseDto1.getUri());
        assertEquals(1L, responseDto1.getHits());

        StatsResponseDto responseDto2 = responseDtos.getLast();
        assertEquals(APP_STATS, responseDto2.getApp());
        assertEquals(URI_TWO, responseDto2.getUri());
        assertEquals(2L, responseDto2.getHits());
    }

    @Test
    void save_sameAppAndUri_multipleHits_returnsAggregatedCount() {
        save(
                hit(APP_EWM, URI_ONE, IP_ONE, BASE_TIME),
                hit(APP_EWM, URI_ONE, IP_TWO, BASE_TIME.plusMinutes(1)),
                hit(APP_EWM, URI_ONE, IP_THREE, BASE_TIME.plusMinutes(2))
        );

        Map<String, Long> hits = hitsByAppAndUri(statsService.getStats(WINDOW_START, WINDOW_END, null, false));

        assertEquals(Map.of(key(APP_EWM, URI_ONE), 3L), hits);
    }

    @Test
    void save_differentAppsSameUri_returnsSeparateGroups() {
        save(
                hit(APP_EWM, URI_ONE, IP_ONE, BASE_TIME),
                hit(APP_STATS, URI_ONE, IP_ONE, BASE_TIME)
        );

        Map<String, Long> hits = hitsByAppAndUri(statsService.getStats(WINDOW_START, WINDOW_END, null, false));

        assertEquals(Map.of(
                key(APP_EWM, URI_ONE), 1L,
                key(APP_STATS, URI_ONE), 1L
        ), hits);
    }

    @Test
    void save_sameAppDifferentUris_returnsSeparateGroups() {
        save(
                hit(APP_EWM, URI_ONE, IP_ONE, BASE_TIME),
                hit(APP_EWM, URI_TWO, IP_ONE, BASE_TIME)
        );

        Map<String, Long> hits = hitsByAppAndUri(statsService.getStats(WINDOW_START, WINDOW_END, null, false));

        assertEquals(Map.of(
                key(APP_EWM, URI_ONE), 1L,
                key(APP_EWM, URI_TWO), 1L
        ), hits);
    }

    @Test
    void save_sameRecordTwice_doesNotDeduplicate() {
        StatsRequestDto dto = hit(APP_EWM, URI_ONE, IP_ONE, BASE_TIME);
        save(dto, dto);

        Map<String, Long> hits = hitsByAppAndUri(statsService.getStats(WINDOW_START, WINDOW_END, null, false));

        assertEquals(Map.of(key(APP_EWM, URI_ONE), 2L), hits);
    }

    @Test
    void save_multipleAppsAndUris_returnsCompleteAggregation() {
        save(
                hit(APP_EWM, URI_ONE, IP_ONE, BASE_TIME),
                hit(APP_EWM, URI_ONE, IP_ONE, BASE_TIME.plusMinutes(1)),
                hit(APP_EWM, URI_ONE, IP_TWO, BASE_TIME.plusMinutes(2)),
                hit(APP_EWM, URI_TWO, IP_ONE, BASE_TIME),
                hit(APP_STATS, URI_ONE, IP_THREE, BASE_TIME),
                hit(APP_STATS, URI_ONE, IP_THREE, BASE_TIME.plusMinutes(5)),
                hit(APP_STATS, URI_TWO, IP_TWO, BASE_TIME)
        );

        Map<String, Long> notUnique = hitsByAppAndUri(statsService.getStats(WINDOW_START, WINDOW_END, null, false));
        Map<String, Long> unique = hitsByAppAndUri(statsService.getStats(WINDOW_START, WINDOW_END, null, true));

        assertEquals(Map.of(
                key(APP_EWM, URI_ONE), 3L,
                key(APP_EWM, URI_TWO), 1L,
                key(APP_STATS, URI_ONE), 2L,
                key(APP_STATS, URI_TWO), 1L
        ), notUnique);

        assertEquals(Map.of(
                key(APP_EWM, URI_ONE), 2L,
                key(APP_EWM, URI_TWO), 1L,
                key(APP_STATS, URI_ONE), 1L,
                key(APP_STATS, URI_TWO), 1L
        ), unique);
    }

    // ---------- подсчёт уникальных IP ----------

    @Test
    void getStats_uniqueTrue_countsDistinctIps() {
        save(
                hit(APP_EWM, URI_ONE, IP_ONE, BASE_TIME),
                hit(APP_EWM, URI_ONE, IP_ONE, BASE_TIME.plusMinutes(1)),
                hit(APP_EWM, URI_ONE, IP_TWO, BASE_TIME.plusMinutes(2))
        );

        Map<String, Long> hits = hitsByAppAndUri(statsService.getStats(WINDOW_START, WINDOW_END, null, true));

        assertEquals(Map.of(key(APP_EWM, URI_ONE), 2L), hits);
    }

    @Test
    void getStats_uniqueFalse_countsAllHits() {
        save(
                hit(APP_EWM, URI_ONE, IP_ONE, BASE_TIME),
                hit(APP_EWM, URI_ONE, IP_ONE, BASE_TIME.plusMinutes(1)),
                hit(APP_EWM, URI_ONE, IP_TWO, BASE_TIME.plusMinutes(2))
        );

        Map<String, Long> hits = hitsByAppAndUri(statsService.getStats(WINDOW_START, WINDOW_END, null, false));

        assertEquals(Map.of(key(APP_EWM, URI_ONE), 3L), hits);
    }

    @Test
    void getStats_uniqueTrue_sameIpInDifferentGroups_countsSeparately() {
        save(
                hit(APP_EWM, URI_ONE, IP_ONE, BASE_TIME),
                hit(APP_STATS, URI_ONE, IP_ONE, BASE_TIME),
                hit(APP_EWM, URI_TWO, IP_ONE, BASE_TIME)
        );

        Map<String, Long> hits = hitsByAppAndUri(statsService.getStats(WINDOW_START, WINDOW_END, null, true));

        assertEquals(Map.of(
                key(APP_EWM, URI_ONE), 1L,
                key(APP_EWM, URI_TWO), 1L,
                key(APP_STATS, URI_ONE), 1L
        ), hits);
    }

    @Test
    void getStats_uniqueTrue_sameIpWithDifferentTimestamps_countsOnce() {
        save(
                hit(APP_EWM, URI_ONE, IP_ONE, BASE_TIME),
                hit(APP_EWM, URI_ONE, IP_ONE, BASE_TIME.plusMinutes(10)),
                hit(APP_EWM, URI_ONE, IP_ONE, BASE_TIME.plusMinutes(20))
        );

        Map<String, Long> hits = hitsByAppAndUri(statsService.getStats(WINDOW_START, WINDOW_END, null, true));

        assertEquals(Map.of(key(APP_EWM, URI_ONE), 1L), hits);
    }

    // ---------- фильтр по uri ----------

    @Test
    void getStats_singleUriFilter_returnsOnlyMatchingGroup() {
        save(
                hit(APP_EWM, URI_ONE, IP_ONE, BASE_TIME),
                hit(APP_STATS, URI_TWO, IP_ONE, BASE_TIME)
        );

        Map<String, Long> hits = hitsByAppAndUri(
                statsService.getStats(WINDOW_START, WINDOW_END, List.of(URI_ONE), false));

        assertEquals(Map.of(key(APP_EWM, URI_ONE), 1L), hits);
    }

    @Test
    void getStats_multipleUriFilter_returnsAllMatchingGroups() {
        save(
                hit(APP_EWM, URI_ONE, IP_ONE, BASE_TIME),
                hit(APP_EWM, URI_TWO, IP_ONE, BASE_TIME),
                hit(APP_STATS, URI_TEN, IP_ONE, BASE_TIME)
        );

        Map<String, Long> hits = hitsByAppAndUri(
                statsService.getStats(WINDOW_START, WINDOW_END, List.of(URI_ONE, URI_TWO), false));

        assertEquals(Map.of(
                key(APP_EWM, URI_ONE), 1L,
                key(APP_EWM, URI_TWO), 1L
        ), hits);
    }

    @Test
    void getStats_unknownUri_returnsEmptyList() {
        statsService.save(hit(APP_EWM, URI_ONE, IP_ONE, BASE_TIME));

        List<StatsResponseDto> responseDtos = statsService.getStats(
                WINDOW_START, WINDOW_END, List.of("/unknown"), false);

        assertNotNull(responseDtos);
        assertTrue(responseDtos.isEmpty());
    }

    @Test
    void getStats_emptyUriList_returnsEmptyList() {
        statsService.save(hit(APP_EWM, URI_ONE, IP_ONE, BASE_TIME));

        List<StatsResponseDto> responseDtos = statsService.getStats(
                WINDOW_START, WINDOW_END, List.of(), false);

        assertNotNull(responseDtos);
        assertTrue(responseDtos.isEmpty());
    }

    @Test
    void getStats_duplicateUrisInFilter_doesNotDuplicateGroups() {
        statsService.save(hit(APP_EWM, URI_ONE, IP_ONE, BASE_TIME));

        Map<String, Long> hits = hitsByAppAndUri(statsService.getStats(
                WINDOW_START, WINDOW_END, List.of(URI_ONE, URI_ONE), false));

        assertEquals(Map.of(key(APP_EWM, URI_ONE), 1L), hits);
    }

    @Test
    void getStats_uriFilterIsExactMatch() {
        save(
                hit(APP_EWM, URI_ONE, IP_ONE, BASE_TIME),
                hit(APP_EWM, URI_TEN, IP_ONE, BASE_TIME)
        );

        Map<String, Long> hits = hitsByAppAndUri(
                statsService.getStats(WINDOW_START, WINDOW_END, List.of(URI_ONE), false));

        assertEquals(Map.of(key(APP_EWM, URI_ONE), 1L), hits);
    }

    @Test
    void getStats_uriFilterWithUniqueTrue_returnsDistinctIpHits() {
        save(
                hit(APP_EWM, URI_ONE, IP_ONE, BASE_TIME),
                hit(APP_EWM, URI_ONE, IP_ONE, BASE_TIME.plusMinutes(1)),
                hit(APP_EWM, URI_ONE, IP_TWO, BASE_TIME.plusMinutes(2)),
                hit(APP_STATS, URI_TWO, IP_THREE, BASE_TIME)
        );

        Map<String, Long> hits = hitsByAppAndUri(statsService.getStats(
                WINDOW_START, WINDOW_END, List.of(URI_ONE), true));

        assertEquals(Map.of(key(APP_EWM, URI_ONE), 2L), hits);
    }

    // ---------- временной диапазон ----------

    @Test
    void getStats_startAndEndAreInclusive() {
        save(
                hit(APP_EWM, URI_ONE, IP_ONE, BASE_TIME),
                hit(APP_EWM, URI_TWO, IP_ONE, BASE_TIME.plusMinutes(10))
        );

        Map<String, Long> hits = hitsByAppAndUri(statsService.getStats(
                BASE_TIME, BASE_TIME.plusMinutes(10), null, false));

        assertEquals(Map.of(
                key(APP_EWM, URI_ONE), 1L,
                key(APP_EWM, URI_TWO), 1L
        ), hits);
    }

    @Test
    void getStats_recordsOutsideRangeExcluded() {
        save(
                hit(APP_EWM, "/before", IP_ONE, BASE_TIME.minusMinutes(1)),
                hit(APP_EWM, "/inside", IP_ONE, BASE_TIME.plusMinutes(5)),
                hit(APP_EWM, "/after", IP_ONE, BASE_TIME.plusMinutes(11))
        );

        Map<String, Long> hits = hitsByAppAndUri(statsService.getStats(
                BASE_TIME, BASE_TIME.plusMinutes(10), null, false));

        assertEquals(Map.of(key(APP_EWM, "/inside"), 1L), hits);
    }

    @Test
    void getStats_startEqualsEnd_returnsRecordAtThatInstant() {
        save(
                hit(APP_EWM, URI_ONE, IP_ONE, BASE_TIME),
                hit(APP_EWM, URI_TWO, IP_ONE, BASE_TIME.plusSeconds(1))
        );

        Map<String, Long> hits = hitsByAppAndUri(statsService.getStats(BASE_TIME, BASE_TIME, null, false));

        assertEquals(Map.of(key(APP_EWM, URI_ONE), 1L), hits);
    }

    @Test
    void getStats_startAfterEnd_returnsEmptyList() {
        statsService.save(hit(APP_EWM, URI_ONE, IP_ONE, BASE_TIME));

        List<StatsResponseDto> responseDtos = statsService.getStats(
                BASE_TIME.plusHours(1), BASE_TIME, null, false);

        assertNotNull(responseDtos);
        assertTrue(responseDtos.isEmpty());
    }

    @Test
    void getStats_noRecordsInRange_returnsEmptyList() {
        List<StatsResponseDto> responseDtos = statsService.getStats(WINDOW_START, WINDOW_END, null, false);

        assertNotNull(responseDtos);
        assertTrue(responseDtos.isEmpty());
    }

    // ---------- маппинг ----------

    @Test
    void save_mapsTimestampToCreatedAt() {
        save(
                hit(APP_EWM, URI_ONE, IP_ONE, BASE_TIME),
                hit(APP_EWM, URI_TWO, IP_ONE, BASE_TIME.plusHours(2))
        );

        Map<String, Long> hits = hitsByAppAndUri(statsService.getStats(BASE_TIME, BASE_TIME, null, false));

        assertEquals(Map.of(key(APP_EWM, URI_ONE), 1L), hits);
    }

    @Test
    void save_persistsIpForUniqueCounting() {
        save(
                hit(APP_EWM, URI_ONE, IP_ONE, BASE_TIME),
                hit(APP_EWM, URI_ONE, IP_ONE, BASE_TIME.plusMinutes(1)),
                hit(APP_EWM, URI_ONE, IP_TWO, BASE_TIME.plusMinutes(2))
        );

        Map<String, Long> hits = hitsByAppAndUri(statsService.getStats(WINDOW_START, WINDOW_END, null, true));

        assertEquals(Map.of(key(APP_EWM, URI_ONE), 2L), hits);
    }

    private void save(StatsRequestDto... hits) {
        Arrays.stream(hits).forEach(statsService::save);
    }

    private static StatsRequestDto hit(String app, String uri, String ip, LocalDateTime timestamp) {
        return StatsRequestDto.builder()
                .app(app)
                .uri(uri)
                .ip(ip)
                .timestamp(timestamp)
                .build();
    }

    private static Map<String, Long> hitsByAppAndUri(List<StatsResponseDto> responseDtos) {
        Map<String, Long> hits = responseDtos.stream()
                .collect(Collectors.toMap(
                        dto -> key(dto.getApp(), dto.getUri()),
                        StatsResponseDto::getHits));

        assertEquals(responseDtos.size(), hits.size(), "Ответ содержит дублирующиеся группы app+uri");

        return hits;
    }

    private static String key(String app, String uri) {
        return app + "|" + uri;
    }
}
