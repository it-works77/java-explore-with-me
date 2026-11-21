package ru.practicum.ewm.category;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.category.dto.CategoryRequestDto;
import ru.practicum.ewm.category.dto.CategoryResponseDto;
import ru.practicum.ewm.category.mapper.CategoryMapper;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.category.repository.CategoryProjection;
import ru.practicum.ewm.category.repository.CategoryRepository;
import ru.practicum.ewm.common.exception.EntityNotFoundException;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public CategoryResponseDto add(CategoryRequestDto requestDto) {
        log.info("Creating category with name {}", requestDto.getName());
        Category category = categoryRepository.save(CategoryMapper.mapCategoryRequestToCategoryDto(requestDto));

        log.debug("Category created with id {}", category.getId());
        return CategoryMapper.mapCategoryToCategoryResponseDto(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponseDto> getAllWithPagination(int offset, int size) {
        log.info("Getting all categories");
        List<CategoryProjection> categories = categoryRepository.findAll(offset, size);
        if (categories.isEmpty()) return List.of();

        log.debug("Found {} categories", categories.size());
        return categories.stream()
                .map(CategoryMapper::mapCategoryProjectionToCategoryResponseDto)
                .toList();
    }

    @Override
    @Transactional
    public CategoryResponseDto update(Long id, CategoryRequestDto requestDto) {
        log.info("Updating category by id {}", id);
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Категория не найдена"));
        category.setName(requestDto.getName());

        log.debug("Category with id {} updated: name = {}", category.getId(), category.getName());
        return CategoryMapper.mapCategoryToCategoryResponseDto(category);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponseDto getById(Long id) {
        log.info("Getting category by id {}", id);
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Категория не найдена"));

        return CategoryMapper.mapCategoryToCategoryResponseDto(category);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        log.info("Deleting category by id {}", id);
        categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Категория не найдена"));
        categoryRepository.deleteById(id);
    }
}
