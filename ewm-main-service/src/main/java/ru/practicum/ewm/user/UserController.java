package ru.practicum.ewm.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.common.exception.IllegalRequestParamException;
import ru.practicum.ewm.user.dto.UserCreateRequestDto;
import ru.practicum.ewm.user.dto.UserResponseDto;

import java.util.List;


@RestController
@Validated
@RequestMapping(path = "/admin/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponseDto add(@Valid @RequestBody UserCreateRequestDto userCreateRequestDto) {
        return userService.add(userCreateRequestDto);
    }

    @GetMapping("")
    public List<UserResponseDto> get(@RequestParam(required = false) List<@Positive Long> ids,
                                     @RequestParam(required = false) @PositiveOrZero Integer from,
                                     @RequestParam(required = false) @Positive Integer size) {
        if (ids != null) {
            return userService.getAllByIds(ids);
        }
        if (from != null && size != null) {
            return userService.getAllWithPagination(from, size);
        }
        throw new IllegalRequestParamException("Либо ids, либо from и size должны быть заданы");
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long userId) {
        userService.remove(userId);
    }
}
