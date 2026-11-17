package ru.practicum.ewm.user;

import ru.practicum.ewm.user.dto.UserCreateRequestDto;
import ru.practicum.ewm.user.dto.UserResponseDto;

import java.util.List;

public interface UserService {
    UserResponseDto add(UserCreateRequestDto userCreateRequestDto);

    UserResponseDto getById(Long id);

    List<UserResponseDto> getAllByIds(List<Long> ids);

    List<UserResponseDto> getAllWithPagination(int offset, int size);

    void remove(Long id);
}
