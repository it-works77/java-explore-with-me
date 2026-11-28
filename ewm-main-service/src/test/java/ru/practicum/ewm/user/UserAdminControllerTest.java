package ru.practicum.ewm.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.common.exception.EntityAlreadyExistsException;
import ru.practicum.ewm.common.exception.EntityNotFoundException;
import ru.practicum.ewm.user.dto.UserCreateRequestDto;
import ru.practicum.ewm.user.dto.UserFullResponseDto;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserAdminController.class)
class UserAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    void add_validBody_returns201WithCreatedUser() throws Exception {
        when(userService.add(any(UserCreateRequestDto.class)))
                .thenReturn(UserFullResponseDto.builder().id(1L).name("Ivan").email("ivan@mail.ru").build());

        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ivan\",\"email\":\"ivan@mail.ru\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Ivan"))
                .andExpect(jsonPath("$.email").value("ivan@mail.ru"));
    }

    @Test
    void add_blankName_returns400AndDoesNotCallService() throws Exception {
        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"email\":\"ivan@mail.ru\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());

        verify(userService, never()).add(any(UserCreateRequestDto.class));
    }

    @Test
    void add_nameLongerThan250_returns400() throws Exception {
        String longName = "x".repeat(251);

        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + longName + "\",\"email\":\"ivan@mail.ru\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());

        verify(userService, never()).add(any(UserCreateRequestDto.class));
    }

    @Test
    void add_invalidEmail_returns400() throws Exception {
        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ivan\",\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists());

        verify(userService, never()).add(any(UserCreateRequestDto.class));
    }

    @Test
    void add_duplicateEmail_returns409() throws Exception {
        when(userService.add(any(UserCreateRequestDto.class)))
                .thenThrow(new EntityAlreadyExistsException("Пользователь с email=ivan@mail.ru уже существует"));

        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ivan\",\"email\":\"ivan@mail.ru\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void add_malformedJson_returns400() throws Exception {
        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ivan\","))
                .andExpect(status().isBadRequest());
    }

    @Test
    void get_withIds_returnsRequestedUsers() throws Exception {
        when(userService.getAllByIds(List.of(1L, 2L)))
                .thenReturn(List.of(UserFullResponseDto.builder().id(1L).name("Ivan").email("ivan@mail.ru").build(),
                        UserFullResponseDto.builder().id(2L).name("Petr").email("petr@mail.ru").build()));

        mockMvc.perform(get("/admin/users").param("ids", "1", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2));

        verify(userService).getAllByIds(List.of(1L, 2L));
        verify(userService, never()).getAllWithPagination(anyInt(), anyInt());
    }

    @Test
    void get_withoutParams_usesDefaultFromAndSize() throws Exception {
        when(userService.getAllWithPagination(0, 10)).thenReturn(List.of());

        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(userService).getAllWithPagination(0, 10);
    }

    @Test
    void get_withFromAndSize_passesThemToService() throws Exception {
        when(userService.getAllWithPagination(5, 2))
                .thenReturn(List.of(UserFullResponseDto.builder().id(6L).name("Ivan").email("ivan@mail.ru").build()));

        mockMvc.perform(get("/admin/users").param("from", "5").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(6));

        verify(userService).getAllWithPagination(5, 2);
    }

    @Test
    void get_withIdsAndPagination_ignoresPagination() throws Exception {
        when(userService.getAllByIds(List.of(1L)))
                .thenReturn(List.of(UserFullResponseDto.builder().id(1L).name("Ivan").email("ivan@mail.ru").build()));

        mockMvc.perform(get("/admin/users")
                        .param("ids", "1")
                        .param("from", "5")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(userService).getAllByIds(List.of(1L));
        verify(userService, never()).getAllWithPagination(anyInt(), anyInt());
    }

    @Test
    void get_withNegativeId_returns400() throws Exception {
        mockMvc.perform(get("/admin/users").param("ids", "-1"))
                .andExpect(status().isBadRequest());

        verify(userService, never()).getAllByIds(any());
    }

    @Test
    void get_withNonNumericIds_returns400() throws Exception {
        mockMvc.perform(get("/admin/users").param("ids", "abc"))
                .andExpect(status().isBadRequest());

        verify(userService, never()).getAllByIds(any());
    }

    @Test
    void get_withNegativeFrom_returns400() throws Exception {
        mockMvc.perform(get("/admin/users").param("from", "-1"))
                .andExpect(status().isBadRequest());

        verify(userService, never()).getAllWithPagination(anyInt(), anyInt());
    }

    @Test
    void get_withZeroSize_returns400() throws Exception {
        mockMvc.perform(get("/admin/users").param("size", "0"))
                .andExpect(status().isBadRequest());

        verify(userService, never()).getAllWithPagination(anyInt(), anyInt());
    }

    @Test
    void delete_existingUser_returns204() throws Exception {
        mockMvc.perform(delete("/admin/users/1"))
                .andExpect(status().isNoContent());

        verify(userService).remove(1L);
    }

    @Test
    void delete_missingUser_returns404() throws Exception {
        doThrow(new EntityNotFoundException("Пользователь не найден по id=999"))
                .when(userService).remove(999L);

        mockMvc.perform(delete("/admin/users/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_nonNumericId_returns400() throws Exception {
        mockMvc.perform(delete("/admin/users/abc"))
                .andExpect(status().isBadRequest());

        verify(userService, never()).remove(any());
    }
}
