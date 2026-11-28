package ru.practicum.ewm.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.user.dto.UserCreateRequestDto;
import ru.practicum.ewm.user.dto.UserFullResponseDto;

import java.util.List;


@RestController
@Validated
@RequestMapping(path = "/admin/users")
@RequiredArgsConstructor
public class UserAdminController {
    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserFullResponseDto add(@Valid @RequestBody UserCreateRequestDto userCreateRequestDto) {
        return userService.add(userCreateRequestDto);
    }

    @GetMapping
    public List<UserFullResponseDto> get(@RequestParam(required = false) List<@Positive Long> ids,
                                         @RequestParam(defaultValue = "0") @PositiveOrZero int from,
                                         @RequestParam(defaultValue = "10") @Positive int size) {
        if (ids != null) {
            // Игнорируем from и size, если ids заданы
            return userService.getAllByIds(ids);
        }

        return userService.getAllWithPagination(from, size);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long userId) {
        userService.remove(userId);
    }
}
