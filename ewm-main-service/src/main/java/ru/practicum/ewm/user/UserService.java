package ru.practicum.ewm.user;

import ru.practicum.ewm.user.dto.UserCreateRequestDto;
import ru.practicum.ewm.user.dto.UserFullResponseDto;

import java.util.List;

public interface UserService {
    UserFullResponseDto add(UserCreateRequestDto userCreateRequestDto);

    UserFullResponseDto getById(Long id);

    List<UserFullResponseDto> getAllByIds(List<Long> ids);

    List<UserFullResponseDto> getAllWithPagination(int offset, int size);

    void remove(Long id);
}
