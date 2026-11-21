package ru.practicum.ewm.category;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.category.dto.CategoryRequestDto;
import ru.practicum.ewm.category.dto.CategoryResponseDto;
import ru.practicum.ewm.common.exception.EntityNotFoundException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CategoryAdminController.class)
class CategoryAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CategoryService categoryService;

    @Test
    void add_validBody_returns201WithCreatedCategory() throws Exception {
        when(categoryService.add(any(CategoryRequestDto.class)))
                .thenReturn(new CategoryResponseDto(1L, "test"));

        mockMvc.perform(post("/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"test\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("test"));
    }

    @Test
    void add_blankName_returns400AndDoesNotCallService() throws Exception {
        mockMvc.perform(post("/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());

        verify(categoryService, never()).add(any(CategoryRequestDto.class));
    }

    @Test
    void add_nameLongerThan50_returns400() throws Exception {
        String longName = "x".repeat(51);

        mockMvc.perform(post("/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + longName + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());

        verify(categoryService, never()).add(any(CategoryRequestDto.class));
    }

    @Test
    void add_duplicateName_returns409() throws Exception {
        when(categoryService.add(any(CategoryRequestDto.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        mockMvc.perform(post("/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"test\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void add_malformedJson_returns400() throws Exception {
        mockMvc.perform(post("/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_validBody_returns200WithUpdatedCategory() throws Exception {
        when(categoryService.update(eq(1L), any(CategoryRequestDto.class)))
                .thenReturn(new CategoryResponseDto(1L, "renamed"));

        mockMvc.perform(patch("/admin/categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"renamed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("renamed"));
    }

    @Test
    void update_missingCategory_returns404() throws Exception {
        when(categoryService.update(eq(999L), any(CategoryRequestDto.class)))
                .thenThrow(new EntityNotFoundException("Категория с id=999 не найдена"));

        mockMvc.perform(patch("/admin/categories/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"renamed\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_nonPositiveId_returns400() throws Exception {
        mockMvc.perform(patch("/admin/categories/0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"renamed\"}"))
                .andExpect(status().isBadRequest());

        verify(categoryService, never()).update(anyLong(), any(CategoryRequestDto.class));
    }

    @Test
    void update_nonNumericId_returns400() throws Exception {
        mockMvc.perform(patch("/admin/categories/abc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"renamed\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void delete_existingCategory_returns204() throws Exception {
        mockMvc.perform(delete("/admin/categories/1"))
                .andExpect(status().isNoContent());

        verify(categoryService).delete(1L);
    }

    @Test
    void delete_missingCategory_returns404() throws Exception {
        doThrow(new EntityNotFoundException("Категория с id=999 не найдена"))
                .when(categoryService).delete(999L);

        mockMvc.perform(delete("/admin/categories/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void put_isNotSupported_returns405() throws Exception {
        mockMvc.perform(put("/admin/categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"test\"}"))
                .andExpect(status().isMethodNotAllowed());
    }
}
