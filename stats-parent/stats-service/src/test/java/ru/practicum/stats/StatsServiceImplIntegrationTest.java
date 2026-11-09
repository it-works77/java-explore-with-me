package ru.practicum.stats;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.stats.dto.StatsRequestDto;
import ru.practicum.stats.dto.StatsResponseDto;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class StatsServiceImplIntegrationTest {
    private final StatsService statsService;

    private static LocalDateTime EVENT_TIME = LocalDateTime.of(2099, 9, 6, 11, 0, 0);
    private static String APP1 = "ewm-main-service";
    private static String APP2 = "stats-service";
    private static String URI1 = "/events/1";
    private static String URI2 = "/events/2";
    private static String IP1 = "192.163.0.1";
    private static String IP2 = "192.163.0.2";

    @Test
    void saveOne_getSaved() {

        StatsRequestDto dto = StatsRequestDto.builder()
                .app(APP1)
                .uri(URI1)
                .ip(IP1)
                .timestamp(EVENT_TIME)
                .build();

        statsService.save(dto);
        List<StatsResponseDto> responseDtos = statsService.getStats(EVENT_TIME.minusHours(1),
                EVENT_TIME.plusHours(1), null, false);

        assertEquals(1, responseDtos.size());

        StatsResponseDto responseDto = responseDtos.getFirst();
        assertEquals(APP1, responseDto.getApp());
        assertEquals(URI1, responseDto.getUri());
        assertEquals(1, responseDto.getHits());
    }

    @Test
    void saveTwo_whenSecondHasTwoHits_getSaved() {

        StatsRequestDto dto1 = StatsRequestDto.builder()
                .app(APP1)
                .uri(URI1)
                .ip(IP1)
                .timestamp(EVENT_TIME)
                .build();

        StatsRequestDto dto2 = StatsRequestDto.builder()
                .app(APP2)
                .uri(URI2)
                .ip(IP1)
                .timestamp(EVENT_TIME.plusMinutes(1))
                .build();

        StatsRequestDto dto3 = StatsRequestDto.builder()
                .app(APP2)
                .uri(URI2)
                .ip(IP2)
                .timestamp(EVENT_TIME.plusMinutes(1))
                .build();

        statsService.save(dto1);
        statsService.save(dto2);
        statsService.save(dto3);

        List<StatsResponseDto> responseDtos = statsService.getStats(EVENT_TIME.minusHours(1),
                EVENT_TIME.plusHours(1), null, false);

        assertEquals(2, responseDtos.size());

        StatsResponseDto responseDto1 = responseDtos.getFirst();
        assertEquals(APP1, responseDto1.getApp());
        assertEquals(URI1, responseDto1.getUri());
        assertEquals(1, responseDto1.getHits());

        StatsResponseDto responseDto2 = responseDtos.getLast();
        assertEquals(APP2, responseDto2.getApp());
        assertEquals(URI2, responseDto2.getUri());
        assertEquals(2, responseDto2.getHits());
    }

    @Test
    void getStats() {
    }
}