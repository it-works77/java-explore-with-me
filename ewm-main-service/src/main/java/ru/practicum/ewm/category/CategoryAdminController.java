package ru.practicum.ewm.category;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.category.dto.CategoryRequestDto;
import ru.practicum.ewm.category.dto.CategoryResponseDto;

@RestController
@Validated
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
public class CategoryAdminController {
    private final CategoryService categoryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponseDto add(@Valid @RequestBody CategoryRequestDto categoryRequestDto) {
        return categoryService.add(categoryRequestDto);
    }

    @PatchMapping("/{catId}")
    public CategoryResponseDto update(@Valid @RequestBody CategoryRequestDto categoryRequestDto,
                                      @PathVariable @Positive Long catId) {
        return categoryService.update(catId, categoryRequestDto);
    }

    @DeleteMapping("/{catId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable @Positive Long catId) {
        categoryService.delete(catId);
    }
}
