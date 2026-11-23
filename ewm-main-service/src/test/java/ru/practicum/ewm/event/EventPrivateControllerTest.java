package ru.practicum.ewm.event;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.common.exception.EntityNotFoundException;
import ru.practicum.ewm.common.exception.EntityUpdateForbiddenException;
import ru.practicum.ewm.event.dto.CreateEventRequestDto;
import ru.practicum.ewm.event.dto.EventFullResponseDto;
import ru.practicum.ewm.event.dto.EventShortResponseDto;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EventPrivateController.class)
class EventPrivateControllerTest {

    private static final String VALID_BODY = "{\"title\":\"Concert\","
            + "\"annotation\":\"Long enough annotation text\","
            + "\"description\":\"Long enough description text\","
            + "\"category\":1,"
            + "\"eventDate\":\"2026-12-31 15:10:05\","
            + "\"location\":{\"lat\":55.75,\"lon\":37.62},"
            + "\"paid\":true,\"participantLimit\":5,\"requestModeration\":false}";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventService eventService;

    @Test
    void add_validBody_returns201() throws Exception {
        when(eventService.add(eq(1L), any(CreateEventRequestDto.class)))
                .thenReturn(EventFullResponseDto.builder().id(10L).title("Concert").build());

        mockMvc.perform(post("/users/1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.title").value("Concert"));
    }

    @Test
    void add_shortTitle_returns400() throws Exception {
        mockMvc.perform(post("/users/1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY.replace("Concert", "ab")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists());

        verify(eventService, never()).add(any(), any());
    }

    @Test
    void add_missingLocation_returns400() throws Exception {
        mockMvc.perform(post("/users/1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY.replace("\"location\":{\"lat\":55.75,\"lon\":37.62},", "")))
                .andExpect(status().isBadRequest());

        verify(eventService, never()).add(any(), any());
    }

    @Test
    void add_nonNumericUserId_returns400() throws Exception {
        mockMvc.perform(post("/users/abc/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isBadRequest());

        verify(eventService, never()).add(any(), any());
    }

    @Test
    void getAll_withoutParams_usesDefaults() throws Exception {
        when(eventService.searchPublic(1L, 0, 10)).thenReturn(List.of());

        mockMvc.perform(get("/users/1/events"))
                .andExpect(status().isOk());

        verify(eventService).searchPublic(1L, 0, 10);
    }

    @Test
    void getAll_withExplicitParams_passesThem() throws Exception {
        when(eventService.searchPublic(1L, 5, 2))
                .thenReturn(List.of(EventShortResponseDto.builder().id(6L).title("E6").build()));

        mockMvc.perform(get("/users/1/events").param("from", "5").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("E6"));

        verify(eventService).searchPublic(1L, 5, 2);
    }

    @Test
    void getAll_negativeFrom_returns400() throws Exception {
        mockMvc.perform(get("/users/1/events").param("from", "-1"))
                .andExpect(status().isBadRequest());

        verify(eventService, never()).searchPublic(any(), anyInt(), anyInt());
    }

    @Test
    void getAll_zeroSize_returns400() throws Exception {
        mockMvc.perform(get("/users/1/events").param("size", "0"))
                .andExpect(status().isBadRequest());

        verify(eventService, never()).searchPublic(any(), anyInt(), anyInt());
    }

    @Test
    void get_existingEvent_returns200() throws Exception {
        when(eventService.get(1L, 2L)).thenReturn(EventFullResponseDto.builder().id(2L).build());

        mockMvc.perform(get("/users/1/events/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2));
    }

    @Test
    void get_missingEvent_returns404() throws Exception {
        when(eventService.get(1L, 999L)).thenThrow(new EntityNotFoundException("not found"));

        mockMvc.perform(get("/users/1/events/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_validBody_returns200() throws Exception {
        when(eventService.update(eq(1L), eq(2L), any()))
                .thenReturn(EventFullResponseDto.builder().id(2L).title("Renamed").build());

        mockMvc.perform(patch("/users/1/events/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Renamed\",\"stateAction\":\"CANCEL_REVIEW\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Renamed"));
    }

    @Test
    void update_invalidTitle_returns400() throws Exception {
        mockMvc.perform(patch("/users/1/events/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"ab\"}"))
                .andExpect(status().isBadRequest());

        verify(eventService, never()).update(any(), any(), any());
    }

    @Test
    void update_serviceForbidden_returns409() throws Exception {
        when(eventService.update(eq(1L), eq(2L), any()))
                .thenThrow(new EntityUpdateForbiddenException("forbidden"));

        mockMvc.perform(patch("/users/1/events/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Renamed\"}"))
                .andExpect(status().isConflict());
    }
}
