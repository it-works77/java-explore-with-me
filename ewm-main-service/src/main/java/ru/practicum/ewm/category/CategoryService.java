package ru.practicum.ewm.category;

import ru.practicum.ewm.category.dto.CategoryRequestDto;
import ru.practicum.ewm.category.dto.CategoryResponseDto;

import java.util.List;

public interface CategoryService {
    CategoryResponseDto add(CategoryRequestDto requestDto);

    CategoryResponseDto getById(Long id);

    List<CategoryResponseDto> getAllWithPagination(int offset, int size);

    CategoryResponseDto update(Long id, CategoryRequestDto requestDto);

    void delete(Long id);
}
