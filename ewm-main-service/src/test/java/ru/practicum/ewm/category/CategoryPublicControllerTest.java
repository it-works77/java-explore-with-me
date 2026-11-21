package ru.practicum.ewm.category;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.category.dto.CategoryResponseDto;
import ru.practicum.ewm.common.exception.EntityNotFoundException;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CategoryPublicController.class)
class CategoryPublicControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CategoryService categoryService;

    @Test
    void getAll_withoutParams_usesDefaultFromAndSize() throws Exception {
        when(categoryService.getAllWithPagination(0, 10)).thenReturn(List.of());

        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(categoryService).getAllWithPagination(0, 10);
    }

    @Test
    void getAll_withExplicitParams_passesThemToService() throws Exception {
        when(categoryService.getAllWithPagination(1, 2))
                .thenReturn(List.of(new CategoryResponseDto(2L, "Cat2")));

        mockMvc.perform(get("/categories")
                        .param("from", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].name").value("Cat2"));

        verify(categoryService).getAllWithPagination(1, 2);
    }

    @Test
    void getAll_withNegativeFrom_returns400() throws Exception {
        mockMvc.perform(get("/categories").param("from", "-1"))
                .andExpect(status().isBadRequest());

        verify(categoryService, never()).getAllWithPagination(anyInt(), anyInt());
    }

    @Test
    void getAll_withZeroSize_returns400() throws Exception {
        mockMvc.perform(get("/categories").param("size", "0"))
                .andExpect(status().isBadRequest());

        verify(categoryService, never()).getAllWithPagination(anyInt(), anyInt());
    }

    @Test
    void getAll_withNonNumericFrom_returns400() throws Exception {
        mockMvc.perform(get("/categories").param("from", "abc"))
                .andExpect(status().isBadRequest());

        verify(categoryService, never()).getAllWithPagination(anyInt(), anyInt());
    }

    @Test
    void getById_existingCategory_returns200() throws Exception {
        when(categoryService.getById(1L)).thenReturn(new CategoryResponseDto(1L, "Concert"));

        mockMvc.perform(get("/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Concert"));
    }

    @Test
    void getById_missingCategory_returns404() throws Exception {
        when(categoryService.getById(999L))
                .thenThrow(new EntityNotFoundException("Категория с id=999 не найдена"));

        mockMvc.perform(get("/categories/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getById_nonPositiveId_returns400() throws Exception {
        mockMvc.perform(get("/categories/0"))
                .andExpect(status().isBadRequest());

        verify(categoryService, never()).getById(0L);
    }

    @Test
    void getById_nonNumericId_returns400() throws Exception {
        mockMvc.perform(get("/categories/abc"))
                .andExpect(status().isBadRequest());

        verify(categoryService, never()).getById(anyLong());
    }
}
