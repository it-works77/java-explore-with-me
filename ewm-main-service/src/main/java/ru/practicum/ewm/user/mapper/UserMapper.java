package ru.practicum.ewm.user.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.user.dto.UserCreateRequestDto;
import ru.practicum.ewm.user.dto.UserFullResponseDto;
import ru.practicum.ewm.user.dto.UserShortResponseDto;
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

    public static UserFullResponseDto mapUserToUserFullResponseDto(User user) {
        return UserFullResponseDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }

    public static UserFullResponseDto mapUserProjectionToUserResponseDto(UserProjection userProjection) {
        return UserFullResponseDto.builder()
                .id(userProjection.getId())
                .name(userProjection.getName())
                .email(userProjection.getEmail())
                .build();
    }


    public static UserShortResponseDto mapUserToUserShortDto(User user) {
        return UserShortResponseDto.builder()
                .id(user.getId())
                .name(user.getName())
                .build();
    }
}
