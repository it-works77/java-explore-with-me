package ru.practicum.ewm.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.common.exception.EntityAlreadyExistsException;
import ru.practicum.ewm.common.exception.EntityNotFoundException;
import ru.practicum.ewm.user.dto.UserCreateRequestDto;
import ru.practicum.ewm.user.dto.UserFullResponseDto;
import ru.practicum.ewm.user.mapper.UserMapper;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserFullResponseDto add(UserCreateRequestDto userCreateRequestDto) {
        log.info("Add user: userDto={}", userCreateRequestDto);
        userRepository.findByEmail(userCreateRequestDto.getEmail()).ifPresent(user -> {
            throw new EntityAlreadyExistsException("Пользователь с email=%s уже существует".formatted(user.getEmail()));
        });

        User user = userRepository.save(UserMapper.mapUserCreateRequestDtoToUser(userCreateRequestDto));
        return UserMapper.mapUserToUserFullResponseDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserFullResponseDto getById(Long id) {
        log.info("Get user: userId={}", id);
        User user = userRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Пользователь не найден по id=%s".formatted(id)));
        return UserMapper.mapUserToUserFullResponseDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserFullResponseDto> getAllByIds(List<Long> ids) {
        log.info("Get users: userIds={}", ids);
        if (ids.isEmpty()) {
            return List.of();
        }

        return userRepository.findAllById(ids).stream()
                .map(UserMapper::mapUserToUserFullResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserFullResponseDto> getAllWithPagination(int offset, int size) {
        log.info("Get users with pagination: offset={}, size={}", offset, size);

        return userRepository.findAll(offset, size).stream()
                .map(UserMapper::mapUserProjectionToUserResponseDto)
                .toList();
    }

    @Override
    @Transactional
    public void remove(Long id) {
        log.info("Remove user: userId={}", id);
        userRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Пользователь не найден по id=%s".formatted(id)));

        userRepository.deleteById(id);
        log.info("User deleted. UserId={}", id);

    }
}
