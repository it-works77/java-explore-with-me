package ru.practicum.ewm.user.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.user.dto.UserCreateRequestDto;
import ru.practicum.ewm.user.dto.UserResponseDto;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserProjection;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UserMapper {
    public static User mapUserCreateRequestDtoToUser(UserCreateRequestDto userCreateRequestDto) {
        return User.builder()
                .name(userCreateRequestDto.getName())
                .email(userCreateRequestDto.getEmail())
                .build();
    }

    public static UserResponseDto mapUserToUserResponseDto(User user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }

    public static UserResponseDto mapUserProjectionToUserResponseDto(UserProjection userProjection) {
        return UserResponseDto.builder()
                .id(userProjection.getId())
                .name(userProjection.getName())
                .email(userProjection.getEmail())
                .build();
    }
}
