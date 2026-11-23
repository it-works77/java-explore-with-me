package ru.practicum.ewm.event;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.common.exception.EntityUpdateForbiddenException;
import ru.practicum.ewm.event.dto.EventFullResponseDto;
import ru.practicum.ewm.event.dto.QueryEventAdminRequestDto;
import ru.practicum.ewm.event.dto.UpdateEventAdminRequestDto;
import ru.practicum.ewm.event.model.AdminStateAction;
import ru.practicum.ewm.event.model.EventState;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EventAdminController.class)
class EventAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventService eventService;

    @Test
    void search_withoutParams_usesDefaultPagination() throws Exception {
        when(eventService.searchAdmin(any(QueryEventAdminRequestDto.class))).thenReturn(List.of());

        mockMvc.perform(get("/admin/events"))
                .andExpect(status().isOk());

        ArgumentCaptor<QueryEventAdminRequestDto> captor = ArgumentCaptor.forClass(QueryEventAdminRequestDto.class);
        verify(eventService).searchAdmin(captor.capture());
        assertThat(captor.getValue().getFrom()).isZero();
        assertThat(captor.getValue().getSize()).isEqualTo(10);
        assertThat(captor.getValue().getUsers()).isNull();
    }

    @Test
    void search_withFilters_passesThemToService() throws Exception {
        when(eventService.searchAdmin(any(QueryEventAdminRequestDto.class)))
                .thenReturn(List.of(EventFullResponseDto.builder().id(1L).title("Concert").build()));

        mockMvc.perform(get("/admin/events")
                        .param("users", "1", "2")
                        .param("states", "PENDING")
                        .param("categories", "3")
                        .param("rangeStart", "2026-12-31 15:10:05")
                        .param("from", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Concert"));

        ArgumentCaptor<QueryEventAdminRequestDto> captor = ArgumentCaptor.forClass(QueryEventAdminRequestDto.class);
        verify(eventService).searchAdmin(captor.capture());
        QueryEventAdminRequestDto query = captor.getValue();
        assertThat(query.getUsers()).containsExactly(1L, 2L);
        assertThat(query.getStates()).containsExactly(EventState.PENDING);
        assertThat(query.getCategories()).containsExactly(3L);
        assertThat(query.getRangeStart()).isEqualTo(LocalDateTime.of(2026, 12, 31, 15, 10, 5));
        assertThat(query.getFrom()).isEqualTo(1);
        assertThat(query.getSize()).isEqualTo(5);
    }

    @Test
    void search_negativeFrom_returns400() throws Exception {
        mockMvc.perform(get("/admin/events").param("from", "-1"))
                .andExpect(status().isBadRequest());

        verify(eventService, never()).searchAdmin(any());
    }

    @Test
    void search_zeroSize_returns400() throws Exception {
        mockMvc.perform(get("/admin/events").param("size", "0"))
                .andExpect(status().isBadRequest());

        verify(eventService, never()).searchAdmin(any());
    }

    @Test
    void search_unknownState_returns400() throws Exception {
        mockMvc.perform(get("/admin/events").param("states", "UNKNOWN"))
                .andExpect(status().isBadRequest());

        verify(eventService, never()).searchAdmin(any());
    }

    @Test
    void search_invalidDate_returns400() throws Exception {
        mockMvc.perform(get("/admin/events").param("rangeStart", "31-12-2026"))
                .andExpect(status().isBadRequest());

        verify(eventService, never()).searchAdmin(any());
    }

    @Test
    void update_validBody_returns200AndPassesDto() throws Exception {
        when(eventService.updateByAdmin(any(), any(UpdateEventAdminRequestDto.class)))
                .thenReturn(EventFullResponseDto.builder().id(1L).title("Renamed").build());

        mockMvc.perform(patch("/admin/events/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Renamed\",\"stateAction\":\"PUBLISH_EVENT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Renamed"));

        ArgumentCaptor<UpdateEventAdminRequestDto> captor =
                ArgumentCaptor.forClass(UpdateEventAdminRequestDto.class);
        verify(eventService).updateByAdmin(any(), captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("Renamed");
        assertThat(captor.getValue().getStateAction()).isEqualTo(AdminStateAction.PUBLISH_EVENT);
        assertThat(captor.getValue().getPaid()).isNull();
    }

    @Test
    void update_malformedJson_returns400() throws Exception {
        mockMvc.perform(patch("/admin/events/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":"))
                .andExpect(status().isBadRequest());

        verify(eventService, never()).updateByAdmin(any(), any());
    }

    @Test
    void update_serviceForbidden_returns409() throws Exception {
        when(eventService.updateByAdmin(any(), any(UpdateEventAdminRequestDto.class)))
                .thenThrow(new EntityUpdateForbiddenException("forbidden"));

        mockMvc.perform(patch("/admin/events/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stateAction\":\"PUBLISH_EVENT\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void update_nonNumericId_returns400() throws Exception {
        mockMvc.perform(patch("/admin/events/abc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stateAction\":\"PUBLISH_EVENT\"}"))
                .andExpect(status().isBadRequest());

        verify(eventService, never()).updateByAdmin(any(), any());
    }
}
