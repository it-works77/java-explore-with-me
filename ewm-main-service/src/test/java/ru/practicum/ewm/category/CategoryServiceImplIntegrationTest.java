package ru.practicum.ewm.category;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.ewm.category.dto.CategoryRequestDto;
import ru.practicum.ewm.category.dto.CategoryResponseDto;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.category.repository.CategoryRepository;
import ru.practicum.ewm.common.exception.EntityNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class CategoryServiceImplIntegrationTest {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CategoryRepository categoryRepository;

    @BeforeEach
    void clearDatabase() {
        categoryRepository.deleteAll();
    }

    @Test
    void add_persistsCategoryAndReturnsGeneratedId() {
        CategoryResponseDto created = categoryService.add(request("Concert"));

        assertThat(created.getId()).isNotNull();
        assertThat(created.getName()).isEqualTo("Concert");
        assertThat(categoryRepository.findById(created.getId()))
                .map(Category::getName)
                .contains("Concert");
    }

    @Test
    void add_duplicateName_throwsDataIntegrityViolationException() {
        categoryService.add(request("Concert"));

        assertThrows(DataIntegrityViolationException.class, () -> categoryService.add(request("Concert")));
    }

    @Test
    void getById_existingCategory_returnsIt() {
        CategoryResponseDto created = categoryService.add(request("Concert"));

        CategoryResponseDto found = categoryService.getById(created.getId());

        assertThat(found.getId()).isEqualTo(created.getId());
        assertThat(found.getName()).isEqualTo("Concert");
    }

    @Test
    void getById_missingCategory_throwsEntityNotFoundException() {
        assertThrows(EntityNotFoundException.class, () -> categoryService.getById(999L));
    }

    @Test
    void update_persistsNewName() {
        CategoryResponseDto created = categoryService.add(request("Concert"));

        CategoryResponseDto updated = categoryService.update(created.getId(), request("Festival"));

        assertThat(updated.getName()).isEqualTo("Festival");
        assertThat(categoryRepository.findById(created.getId()))
                .map(Category::getName)
                .contains("Festival");
    }

    @Test
    void update_missingCategory_throwsEntityNotFoundException() {
        assertThrows(EntityNotFoundException.class, () -> categoryService.update(999L, request("Festival")));
    }

    @Test
    void update_toDuplicateName_throwsDataIntegrityViolationException() {
        categoryService.add(request("Concert"));
        CategoryResponseDto second = categoryService.add(request("Festival"));

        assertThrows(DataIntegrityViolationException.class,
                () -> categoryService.update(second.getId(), request("Concert")));
    }

    @Test
    void delete_existingCategory_removesIt() {
        CategoryResponseDto created = categoryService.add(request("Concert"));

        categoryService.delete(created.getId());

        assertThat(categoryRepository.findById(created.getId())).isEmpty();
    }

    @Test
    void delete_missingCategory_throwsEntityNotFoundException() {
        assertThrows(EntityNotFoundException.class, () -> categoryService.delete(999L));
    }

    @Test
    void getAllWithPagination_returnsRequestedPageOrderedById() {
        for (int i = 1; i <= 5; i++) {
            categoryService.add(request("Cat" + i));
        }

        assertThat(categoryService.getAllWithPagination(1, 3))
                .extracting(CategoryResponseDto::getName)
                .containsExactly("Cat2", "Cat3", "Cat4");
    }

    @Test
    void getAllWithPagination_offsetBeyondLastPage_returnsEmptyList() {
        categoryService.add(request("Cat1"));

        assertThat(categoryService.getAllWithPagination(10, 10)).isEmpty();
    }

    private CategoryRequestDto request(String name) {
        CategoryRequestDto requestDto = new CategoryRequestDto();
        requestDto.setName(name);
        return requestDto;
    }
}
