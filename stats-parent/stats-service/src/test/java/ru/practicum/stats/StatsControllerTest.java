package ru.practicum.stats;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.stats.dto.GetStatsRequestDto;
import ru.practicum.stats.dto.SaveStatsRequestDto;
import ru.practicum.stats.dto.StatsResponseDto;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = StatsController.class)
class StatsControllerTest {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final LocalDateTime START = LocalDateTime.of(2024, 9, 6, 11, 0, 0);
    private static final LocalDateTime END = LocalDateTime.of(2024, 9, 6, 12, 0, 0);
    private static final String START_PARAM = "2024-09-06 11:00:00";
    private static final String END_PARAM = "2024-09-06 12:00:00";

    private static final String APP = "ewm-main-service";
    private static final String OTHER_APP = "stats-service";
    private static final String URI_ONE = "/events/1";
    private static final String URI_TWO = "/events/2";
    private static final String IP = "192.163.0.1";

    @MockBean
    StatsService statsService;

    @Autowired
    MockMvc mockMvc;

    @Nested
    class PostHit {

        @Test
        void postHit_validBody_returns201WithEmptyBody() throws Exception {
            mockMvc.perform(post("/hit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(hitJson(APP, URI_ONE, IP, START_PARAM)))
                    .andExpect(status().isCreated())
                    .andExpect(content().string(""));
        }

        @Test
        void postHit_validBody_passesParsedDtoToService() throws Exception {
            mockMvc.perform(post("/hit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(hitJson(APP, URI_ONE, IP, START_PARAM)))
                    .andExpect(status().isCreated());

            ArgumentCaptor<SaveStatsRequestDto> captor = ArgumentCaptor.forClass(SaveStatsRequestDto.class);
            verify(statsService).save(captor.capture());
            verifyNoMoreInteractions(statsService);

            SaveStatsRequestDto saved = captor.getValue();
            assertEquals(APP, saved.getApp());
            assertEquals(URI_ONE, saved.getUri());
            assertEquals(IP, saved.getIp());
            assertEquals(START, saved.getTimestamp());
        }

        @Test
        void postHit_presentTimestamp_returns201() throws Exception {
            String now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).format(DATE_FORMAT);

            mockMvc.perform(post("/hit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(hitJson(APP, URI_ONE, IP, now)))
                    .andExpect(status().isCreated());
        }

        @Test
        void postHit_unknownFieldInBody_isIgnored_returns201() throws Exception {
            String body = "{\"id\":1,\"app\":\"" + APP + "\",\"uri\":\"" + URI_ONE
                    + "\",\"ip\":\"" + IP + "\",\"timestamp\":\"" + START_PARAM + "\"}";

            mockMvc.perform(post("/hit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated());
        }
    }

    @Nested
    class PostHitDateFormat {

        @Test
        void postHit_documentedDateFormat_isAccepted() throws Exception {
            mockMvc.perform(post("/hit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(hitJson(APP, URI_ONE, IP, "2024-09-06 11:00:00")))
                    .andExpect(status().isCreated());

            ArgumentCaptor<SaveStatsRequestDto> captor = ArgumentCaptor.forClass(SaveStatsRequestDto.class);
            verify(statsService).save(captor.capture());
            assertEquals(LocalDateTime.of(2024, 9, 6, 11, 0, 0), captor.getValue().getTimestamp());
        }

        @Test
        void postHit_isoTimestamp_returns400() throws Exception {
            mockMvc.perform(post("/hit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(hitJson(APP, URI_ONE, IP, "2024-09-06T11:00:00")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Ошибка парсинга запроса"))
                    .andExpect(jsonPath("$.details").exists());

            verifyNoInteractions(statsService);
        }

        @Test
        void postHit_timestampWithoutSeconds_returns400() throws Exception {
            mockMvc.perform(post("/hit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(hitJson(APP, URI_ONE, IP, "2024-09-06 11:00")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Ошибка парсинга запроса"));

            verifyNoInteractions(statsService);
        }

        @Test
        void postHit_unparsableTimestamp_returns400() throws Exception {
            mockMvc.perform(post("/hit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(hitJson(APP, URI_ONE, IP, "not-a-date")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Ошибка парсинга запроса"));

            verifyNoInteractions(statsService);
        }

        @Test
        void postHit_futureTimestamp_returns400() throws Exception {
            mockMvc.perform(post("/hit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(hitJson(APP, URI_ONE, IP, "2999-01-01 00:00:00")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Ошибка валидации"))
                    .andExpect(jsonPath("$.errors.timestamp").exists());

            verifyNoInteractions(statsService);
        }
    }

    @Nested
    class PostHitValidation {

        @Test
        void postHit_blankApp_returns400() throws Exception {
            mockMvc.perform(post("/hit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(hitJson("", URI_ONE, IP, START_PARAM)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Ошибка валидации"))
                    .andExpect(jsonPath("$.errors.app").exists());

            verifyNoInteractions(statsService);
        }

        @Test
        void postHit_blankUri_returns400() throws Exception {
            mockMvc.perform(post("/hit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(hitJson(APP, "", IP, START_PARAM)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.uri").exists());

            verifyNoInteractions(statsService);
        }

        @Test
        void postHit_blankIp_returns400() throws Exception {
            mockMvc.perform(post("/hit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(hitJson(APP, URI_ONE, "", START_PARAM)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.ip").exists());

            verifyNoInteractions(statsService);
        }

        @Test
        void postHit_missingTimestamp_returns400() throws Exception {
            String body = "{\"app\":\"" + APP + "\",\"uri\":\"" + URI_ONE + "\",\"ip\":\"" + IP + "\"}";

            mockMvc.perform(post("/hit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.timestamp").exists());

            verifyNoInteractions(statsService);
        }

        @Test
        void postHit_emptyObject_returns400WithAllFieldErrors() throws Exception {
            mockMvc.perform(post("/hit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Ошибка валидации"))
                    .andExpect(jsonPath("$.errors.app").exists())
                    .andExpect(jsonPath("$.errors.uri").exists())
                    .andExpect(jsonPath("$.errors.ip").exists())
                    .andExpect(jsonPath("$.errors.timestamp").exists());

            verifyNoInteractions(statsService);
        }

        @Test
        void postHit_malformedJson_returns400() throws Exception {
            mockMvc.perform(post("/hit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"app\":\"ewm-main-service\","))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Ошибка парсинга запроса"));

            verifyNoInteractions(statsService);
        }
    }

    @Nested
    class ProtocolErrors {

        @Test
        void postHit_wrongContentType_returns500InsteadOf415() throws Exception {
            mockMvc.perform(post("/hit")
                            .contentType(MediaType.TEXT_PLAIN)
                            .content("app=ewm-main-service"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.error").value("Внутренняя ошибка сервера"));
        }

        @Test
        void getOnHitEndpoint_returns500InsteadOf405() throws Exception {
            mockMvc.perform(get("/hit"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.details", containsString("Request method 'GET' is not supported")));
        }

        @Test
        void getUnknownPath_returns500InsteadOf404() throws Exception {
            mockMvc.perform(get("/unknown"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.details", containsString("No static resource")));
        }
    }

    @Nested
    class GetStats {

        @Test
        void getStats_returnsBodyFromService() throws Exception {
            when(statsService.getStats(any(GetStatsRequestDto.class)))
                    .thenReturn(List.of(
                            response(APP, URI_ONE, 3L),
                            response(OTHER_APP, URI_TWO, 1L)));

            mockMvc.perform(get("/stats")
                            .param("start", START_PARAM)
                            .param("end", END_PARAM))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].app").value(APP))
                    .andExpect(jsonPath("$[0].uri").value(URI_ONE))
                    .andExpect(jsonPath("$[0].hits").isNumber())
                    .andExpect(jsonPath("$[0].hits").value(3))
                    .andExpect(jsonPath("$[1].app").value(OTHER_APP));
        }

        @Test
        void getStats_passesAllParamsToService() throws Exception {
            mockMvc.perform(get("/stats")
                            .param("start", START_PARAM)
                            .param("end", END_PARAM)
                            .param("uris", URI_ONE, URI_TWO)
                            .param("unique", "true"))
                    .andExpect(status().isOk());

            GetStatsRequestDto request = captureGetStatsRequest();
            assertEquals(START, request.getStart());
            assertEquals(END, request.getEnd());
            assertEquals(List.of(URI_ONE, URI_TWO), request.getUris());
            assertTrue(request.isUnique());
        }

        @Test
        void getStats_urisRepeatedParam_parsedAsList() throws Exception {
            mockMvc.perform(get("/stats")
                            .param("start", START_PARAM)
                            .param("end", END_PARAM)
                            .queryParam("uris", URI_ONE)
                            .queryParam("uris", URI_TWO))
                    .andExpect(status().isOk());

            GetStatsRequestDto request = captureGetStatsRequest();
            assertEquals(List.of(URI_ONE, URI_TWO), request.getUris());
            assertFalse(request.isUnique());
        }

        @Test
        void getStats_urisUrlEncoded_isDecoded() throws Exception {
            mockMvc.perform(get(URI.create("/stats?start=2024-09-06%2011%3A00%3A00"
                            + "&end=2024-09-06%2012%3A00%3A00&uris=%2Fevents%2F1")))
                    .andExpect(status().isOk());

            GetStatsRequestDto request = captureGetStatsRequest();
            assertEquals(List.of(URI_ONE), request.getUris());
            assertEquals(START, request.getStart());
            assertEquals(END, request.getEnd());
        }

        @Test
        void getStats_uniqueDefaultsToFalse() throws Exception {
            mockMvc.perform(get("/stats")
                            .param("start", START_PARAM)
                            .param("end", END_PARAM))
                    .andExpect(status().isOk());

            GetStatsRequestDto request = captureGetStatsRequest();
            assertNull(request.getUris());
            assertFalse(request.isUnique());
        }

        @Test
        void getStats_emptyResult_returnsEmptyArray() throws Exception {
            when(statsService.getStats(any(GetStatsRequestDto.class))).thenReturn(List.of());

            mockMvc.perform(get("/stats")
                            .param("start", START_PARAM)
                            .param("end", END_PARAM))
                    .andExpect(status().isOk())
                    .andExpect(content().json("[]"));
        }
    }

    @Nested
    class GetStatsErrors {

        @Test
        void getStats_missingStart_returns400() throws Exception {
            mockMvc.perform(get("/stats").param("end", END_PARAM))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Отсутствует обязательный параметр"))
                    .andExpect(jsonPath("$.details", containsString("start")));

            verifyNoInteractions(statsService);
        }

        @Test
        void getStats_missingEnd_returns400() throws Exception {
            mockMvc.perform(get("/stats").param("start", START_PARAM))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Отсутствует обязательный параметр"))
                    .andExpect(jsonPath("$.details", containsString("end")));

            verifyNoInteractions(statsService);
        }

        @Test
        void getStats_invalidStartFormat_returns400() throws Exception {
            mockMvc.perform(get("/stats")
                            .param("start", "2024-09-06")
                            .param("end", END_PARAM))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Ошибка парсинга запроса"));

            verifyNoInteractions(statsService);
        }

        @Test
        void getStats_invalidUniqueValue_returns400() throws Exception {
            mockMvc.perform(get("/stats")
                            .param("start", START_PARAM)
                            .param("end", END_PARAM)
                            .param("unique", "maybe"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Ошибка парсинга запроса"));

            verifyNoInteractions(statsService);
        }
    }

    @Nested
    class ServiceFailures {

        @Test
        void postHit_serviceThrows_returns500() throws Exception {
            doThrow(new RuntimeException("boom")).when(statsService).save(any());

            mockMvc.perform(post("/hit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(hitJson(APP, URI_ONE, IP, START_PARAM)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.error").value("Внутренняя ошибка сервера"))
                    .andExpect(jsonPath("$.details").value("boom"));
        }

        @Test
        void getStats_serviceThrows_returns500() throws Exception {
            when(statsService.getStats(any(GetStatsRequestDto.class)))
                    .thenThrow(new RuntimeException("boom"));

            mockMvc.perform(get("/stats")
                            .param("start", START_PARAM)
                            .param("end", END_PARAM))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.error").value("Внутренняя ошибка сервера"));
        }
    }

    private GetStatsRequestDto captureGetStatsRequest() {
        ArgumentCaptor<GetStatsRequestDto> captor = ArgumentCaptor.forClass(GetStatsRequestDto.class);
        verify(statsService).getStats(captor.capture());
        verifyNoMoreInteractions(statsService);

        return captor.getValue();
    }

    private static String hitJson(String app, String uri, String ip, String timestamp) {
        return "{\"app\":\"" + app + "\",\"uri\":\"" + uri + "\",\"ip\":\"" + ip
                + "\",\"timestamp\":\"" + timestamp + "\"}";
    }

    private static StatsResponseDto response(String app, String uri, Long hits) {
        return StatsResponseDto.builder()
                .app(app)
                .uri(uri)
                .hits(hits)
                .build();
    }
}
