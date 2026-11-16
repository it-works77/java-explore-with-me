package ru.practicum.stats.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StatsResponseDtoTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String RESPONSE_JSON = "{\"app\": \"ewm-main-service\", \"uri\": \"/events/1\", "
            + "\"hits\": 5}";

    @Test
    void shouldDeserializeResponseDto() throws JsonProcessingException {
        StatsResponseDto dto = OBJECT_MAPPER.readValue(RESPONSE_JSON, StatsResponseDto.class);

        assertThat(dto.getApp()).isEqualTo("ewm-main-service");
        assertThat(dto.getUri()).isEqualTo("/events/1");
        assertThat(dto.getHits()).isEqualTo(5L);
    }
}
