package ru.practicum.stats.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class StatsRequestDtoTest {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .build();

    private static final String APP = "ewm-main-service";
    private static final String URI = "/events/1";
    private static final String IP = "192.163.0.1";
    private static final LocalDateTime TIMESTAMP = LocalDateTime.of(2022, 9, 6, 11, 0, 23);

    private static final String REQUEST_JSON = "{\"app\": \"ewm-main-service\", \"uri\": \"/events/1\", "
            + "\"ip\": \"192.163.0.1\", \"timestamp\": \"2022-09-06 11:00:23\"}";

    @Test
    void shouldDeserializeRequestDto() throws JsonProcessingException {
        StatsRequestDto dto = OBJECT_MAPPER.readValue(REQUEST_JSON, StatsRequestDto.class);

        assertThat(dto.getApp()).isEqualTo(APP);
        assertThat(dto.getUri()).isEqualTo(URI);
        assertThat(dto.getIp()).isEqualTo(IP);
        assertThat(dto.getTimestamp()).isEqualTo(TIMESTAMP);
    }

    @Test
    void shouldSerializeRequestDtoWithDocumentedDateFormat() throws JsonProcessingException {
        StatsRequestDto dto = StatsRequestDto.builder()
                .app(APP)
                .uri(URI)
                .ip(IP)
                .timestamp(TIMESTAMP)
                .build();

        String actualJson = OBJECT_MAPPER.writeValueAsString(dto);

        assertThat(OBJECT_MAPPER.readTree(actualJson))
                .isEqualTo(OBJECT_MAPPER.readTree(REQUEST_JSON));
    }
}
