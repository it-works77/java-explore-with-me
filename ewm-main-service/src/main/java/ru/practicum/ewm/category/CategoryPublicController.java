package ru.practicum.ewm.category;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.category.dto.CategoryResponseDto;

import java.util.List;

@RestController
@Validated
@RequestMapping(path = "/categories")
@RequiredArgsConstructor
public class CategoryPublicController {
    private final CategoryService categoryService;

    @GetMapping("/{catId}")
    public CategoryResponseDto getById(@PathVariable @Positive Long catId) {
        return categoryService.getById(catId);
    }

    @GetMapping
    public List<CategoryResponseDto> getAllWithPagination(@RequestParam(defaultValue = "0") @PositiveOrZero int from,
                                            @RequestParam(defaultValue = "10") @Positive int size) {
        return categoryService.getAllWithPagination(from, size);
    }
}
