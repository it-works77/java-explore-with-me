package ru.practicum.stats.client;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import ru.practicum.stats.client.exception.StatsClientException;
import ru.practicum.stats.client.exception.StatsServiceUnavailableException;
import ru.practicum.stats.dto.GetStatsRequestDto;
import ru.practicum.stats.dto.SaveStatsRequestDto;
import ru.practicum.stats.dto.StatsResponseDto;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.anything;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class StatsClientImplTest {
    private static final String BASE_URL = "http://localhost:9090";
    private static final LocalDateTime START = LocalDateTime.of(2022, 9, 6, 11, 0, 23);
    private static final LocalDateTime END = START.plusDays(1);

    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private StatsClientImpl client;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        client = new StatsClientImpl(restTemplate, BASE_URL);
    }

    // ---------------------------------------------------------------- URI / контракт

    @Test
    void shouldFormatDatesAsContractRequires() {
        URI uri = client.buildStatsUri(new GetStatsRequestDto(START, END, null, true));

        assertThat(uri.getRawQuery())
                .contains("start=2022-09-06%2011:00:23")
                .contains("end=2022-09-07%2011:00:23")
                .doesNotContain("T11");
    }

    @Test
    void shouldOmitUrisWhenNull() {
        assertThat(client.buildStatsUri(new GetStatsRequestDto(START, END, null, true)).getRawQuery())
                .doesNotContain("uris");
    }

    @Test
    void shouldOmitUrisWhenEmpty() {
        assertThat(client.buildStatsUri(new GetStatsRequestDto(START, END, List.of(), true)).getRawQuery())
                .doesNotContain("uris");
    }

    @Test
    void shouldRepeatUrisWhenPresent() {
        assertThat(client.buildStatsUri(new GetStatsRequestDto(START, END,
                List.of("/events/1", "/events/2"), false)).getRawQuery())
                .contains("uris=/events/1")
                .contains("uris=/events/2")
                .contains("unique=false");
    }

    @Test
    void shouldIgnoreTrailingSlashInBaseUrl() {
        StatsClientImpl clientWithSlash = new StatsClientImpl(restTemplate, BASE_URL + "/");

        URI uri = clientWithSlash.buildStatsUri(new GetStatsRequestDto(START, END, null, true));

        assertThat(uri.getPath()).isEqualTo("/stats");
        assertThat(uri.getHost()).isEqualTo("localhost");
    }

    @Test
    void shouldTrimBaseUrl() {
        StatsClientImpl paddedClient = new StatsClientImpl(restTemplate, "  " + BASE_URL + "  ");

        URI uri = paddedClient.buildStatsUri(new GetStatsRequestDto(START, END, null, true));

        assertThat(uri.getPort()).isEqualTo(9090);
        assertThat(uri.getPath()).isEqualTo("/stats");
    }

    // ---------------------------------------------------------------- конструктор

    @Test
    void shouldRejectNullRestTemplate() {
        assertThatNullPointerException().isThrownBy(() -> new StatsClientImpl(null, BASE_URL));
    }

    @Test
    void shouldRejectNullOrBlankUrl() {
        assertThatThrownBy(() -> new StatsClientImpl(restTemplate, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new StatsClientImpl(restTemplate, "   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---------------------------------------------------------------- saveHit

    @Test
    void shouldPostHitAsJson() {
        server.expect(once(), requestTo(BASE_URL + "/hit"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.app").value("ewm-main-service"))
                .andExpect(jsonPath("$.uri").value("/events/1"))
                .andExpect(jsonPath("$.ip").value("127.0.0.1"))
                .andExpect(jsonPath("$.timestamp").value("2022-09-06 11:00:23"))
                .andRespond(withStatus(HttpStatus.CREATED));

        client.saveHit(hitDto());

        server.verify();
    }

    @Test
    void shouldRejectNullDtoOnSaveHit() {
        assertThatNullPointerException().isThrownBy(() -> client.saveHit(null));
    }

    @Test
    void shouldThrowClientExceptionOnSaveHit4xx() {
        server.expect(once(), anything()).andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> client.saveHit(hitDto()))
                .isInstanceOf(StatsClientException.class)
                .isNotInstanceOf(StatsServiceUnavailableException.class)
                .hasMessageContaining("400");
    }

    @Test
    void shouldThrowUnavailableOnSaveHit5xx() {
        server.expect(once(), anything()).andRespond(withServerError());

        assertThatThrownBy(() -> client.saveHit(hitDto()))
                .isInstanceOf(StatsServiceUnavailableException.class)
                .hasMessageContaining("500");
    }

    @Test
    void shouldThrowClientExceptionOnSaveHitConnectionFailure() {
        server.expect(once(), anything()).andRespond(withException(new ConnectException("Connection refused")));

        assertThatThrownBy(() -> client.saveHit(hitDto()))
                .isInstanceOf(StatsClientException.class);
    }

    // ---------------------------------------------------------------- getStats

    @Test
    void shouldReturnParsedStats() {
        server.expect(once(), anything())
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "[{\"app\":\"ewm-main-service\",\"uri\":\"/events/1\",\"hits\":7}]",
                        MediaType.APPLICATION_JSON));

        List<StatsResponseDto> stats = client.getStats(statsQuery());

        assertThat(stats).singleElement().satisfies(dto -> {
            assertThat(dto.getApp()).isEqualTo("ewm-main-service");
            assertThat(dto.getUri()).isEqualTo("/events/1");
            assertThat(dto.getHits()).isEqualTo(7L);
        });
        server.verify();
    }

    @Test
    void shouldReturnEmptyListWhenBodyIsAbsent() {
        server.expect(once(), anything()).andRespond(withStatus(HttpStatus.OK));

        assertThat(client.getStats(statsQuery())).isEmpty();
    }

    @Test
    void shouldRejectNullDtoOnGetStats() {
        assertThatNullPointerException().isThrownBy(() -> client.getStats(null));
    }

    @Test
    void shouldThrowClientExceptionOnGetStats4xx() {
        server.expect(once(), anything())
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .body("{\"error\":\"start must not be after end\"}"));

        assertThatThrownBy(() -> client.getStats(statsQuery()))
                .isInstanceOf(StatsClientException.class)
                .isNotInstanceOf(StatsServiceUnavailableException.class)
                .hasMessageContaining("400")
                .hasMessageContaining("start must not be after end");
    }

    @Test
    void shouldThrowUnavailableOnGetStats5xx() {
        server.expect(once(), anything()).andRespond(withServerError());

        assertThatThrownBy(() -> client.getStats(statsQuery()))
                .isInstanceOf(StatsServiceUnavailableException.class)
                .hasMessageContaining("500");
    }

    @Test
    void shouldThrowClientExceptionOnGetStatsConnectionFailure() {
        server.expect(once(), anything()).andRespond(withException(new ConnectException("Connection refused")));

        assertThatThrownBy(() -> client.getStats(statsQuery()))
                .isInstanceOf(StatsClientException.class);
    }

    // ---------------------------------------------------------------- withDefaults

    @Test
    void shouldWorkOverRealHttpWithDefaults() throws IOException {
        List<String> hitBodies = new CopyOnWriteArrayList<>();
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(0), 0);
        httpServer.createContext("/hit", exchange -> {
            hitBodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(HttpStatus.CREATED.value(), -1);
            exchange.close();
        });
        httpServer.createContext("/stats", exchange -> {
            byte[] body = "[{\"app\":\"ewm-main-service\",\"uri\":\"/events/1\",\"hits\":7}]"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", MediaType.APPLICATION_JSON_VALUE);
            exchange.sendResponseHeaders(HttpStatus.OK.value(), body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        httpServer.start();
        try {
            StatsClientImpl defaultClient = StatsClientImpl.withDefaults(
                    "http://localhost:" + httpServer.getAddress().getPort());

            defaultClient.saveHit(hitDto());
            List<StatsResponseDto> stats = defaultClient.getStats(statsQuery());

            assertThat(hitBodies).singleElement().asString()
                    .contains("\"timestamp\":\"2022-09-06 11:00:23\"");
            assertThat(stats).singleElement().extracting(StatsResponseDto::getHits).isEqualTo(7L);
        } finally {
            httpServer.stop(0);
        }
    }

    // ---------------------------------------------------------------- helpers

    private static SaveStatsRequestDto hitDto() {
        return SaveStatsRequestDto.builder()
                .app("ewm-main-service")
                .uri("/events/1")
                .ip("127.0.0.1")
                .timestamp(START)
                .build();
    }

    private static GetStatsRequestDto statsQuery() {
        return new GetStatsRequestDto(START, END, List.of("/events/1"), true);
    }
}
