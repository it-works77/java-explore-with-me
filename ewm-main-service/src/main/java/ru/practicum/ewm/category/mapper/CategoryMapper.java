package ru.practicum.ewm.category.mapper;


import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.category.dto.CategoryRequestDto;
import ru.practicum.ewm.category.dto.CategoryResponseDto;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.category.repository.CategoryProjection;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CategoryMapper {
    public static CategoryResponseDto mapCategoryToCategoryResponseDto(Category category) {
        return new CategoryResponseDto(category.getId(), category.getName());
    }

    public static Category mapCategoryRequestToCategoryDto(CategoryRequestDto dto) {
        return Category.builder()
                .name(dto.getName())
                .build();
    }

    public static CategoryResponseDto mapCategoryProjectionToCategoryResponseDto(CategoryProjection projection) {
        return new CategoryResponseDto(projection.getId(), projection.getName());
    }
}
