package ru.practicum.stats.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import java.net.http.HttpClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.stats.api.StatsApiConfig;
import ru.practicum.stats.client.exception.StatsClientException;
import ru.practicum.stats.client.exception.StatsServiceUnavailableException;
import ru.practicum.stats.dto.GetStatsRequestDto;
import ru.practicum.stats.dto.SaveStatsRequestDto;
import ru.practicum.stats.dto.StatsResponseDto;

import java.net.URI;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.List;

import java.util.Locale;
import java.util.Objects;

@Slf4j
public final class StatsClientImpl implements StatsClient {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter
            .ofPattern(StatsApiConfig.DATE_TIME_PATTERN, Locale.ROOT);
    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(5);

    private final RestTemplate restTemplate;
    private final URI saveHitUri;
    private final URI getStatsUri;

    public StatsClientImpl(RestTemplate restTemplate, String statsServiceUrl) {
        this.restTemplate = Objects.requireNonNull(restTemplate, "restTemplate must not be null");

        if (statsServiceUrl == null || statsServiceUrl.isBlank()) {
            throw new IllegalArgumentException("Stats service url is null");
        }
        statsServiceUrl = statsServiceUrl.strip();

        this.saveHitUri = UriComponentsBuilder.fromUriString(statsServiceUrl)
                .path("/hit")
                .build()
                .toUri();
        this.getStatsUri = UriComponentsBuilder.fromUriString(statsServiceUrl)
                .path("/stats")
                .build()
                .toUri();
    }

    public static StatsClientImpl withDefaults(String statsServiceUrl) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(DEFAULT_CONNECT_TIMEOUT)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(DEFAULT_READ_TIMEOUT);

        return new StatsClientImpl(new RestTemplate(requestFactory), statsServiceUrl);
    }

    @Override
    public void saveHit(SaveStatsRequestDto dto) {
        Objects.requireNonNull(dto, "dto must not be null");

        log.debug("Saving hit for uri={}", dto.getUri());
        try {
            restTemplate.postForEntity(saveHitUri, dto, Void.class);
        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode().is5xxServerError()) {
                throw new StatsServiceUnavailableException("stats-service POST hit returned " + e.getStatusCode().value(), e);
            }
            throw new StatsClientException("stats-service rejected POST hit request for uri " + dto.getUri()
                    + " with status " + e.getStatusCode().value() + " and body " + e.getResponseBodyAsString(), e);
        } catch (RestClientException e) {
            throw new StatsClientException("Failed to send hit to" + dto.getUri(), e);
        }
    }

    @Override
    public List<StatsResponseDto> getStats(GetStatsRequestDto dto) {
        log.debug("Getting stats {}", dto);

        Objects.requireNonNull(dto, "dto must not be null");

        URI uri = buildStatsUri(dto);
        log.debug("URI: {}", uri);

        ResponseEntity<List<StatsResponseDto>> response;
        try {
            response = restTemplate.exchange(uri,
                    HttpMethod.GET,
                    HttpEntity.EMPTY,
                    new ParameterizedTypeReference<>() {
                    }
            );
        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode().is5xxServerError()) {
                throw new StatsServiceUnavailableException("stats-service GET stats returned " + e.getStatusCode().value(), e);
            }
            throw new StatsClientException("stats-service rejected GET stats request for uri " + uri
                    + " with status " + e.getStatusCode().value() + " and body " + e.getResponseBodyAsString(), e);
        } catch (RestClientException e) {
            throw new StatsClientException("Failed get stats from " + uri, e);
        }

        List<StatsResponseDto> body = response.getBody();
        return body == null ? List.of() : body;
    }

    URI buildStatsUri(GetStatsRequestDto dto) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUri(getStatsUri)
                .queryParam("start", DATE_TIME_FORMATTER.format(dto.getStart()))
                .queryParam("end", DATE_TIME_FORMATTER.format(dto.getEnd()))
                .queryParam("unique", dto.isUnique());
        if (dto.getUris() != null && !dto.getUris().isEmpty()) {
            uriBuilder.queryParam("uris", dto.getUris());
        }
        return uriBuilder.build().toUri();
    }
}
