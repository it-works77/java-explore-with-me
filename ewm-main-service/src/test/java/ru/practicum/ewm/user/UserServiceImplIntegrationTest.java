package ru.practicum.ewm.user;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.ewm.common.exception.EntityAlreadyExistsException;
import ru.practicum.ewm.common.exception.EntityNotFoundException;
import ru.practicum.ewm.user.dto.UserCreateRequestDto;
import ru.practicum.ewm.user.dto.UserFullResponseDto;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class UserServiceImplIntegrationTest {

    private final UserService userService;
    private final UserRepository userRepository;

    @BeforeEach
    void clearDatabase() {
        userRepository.deleteAll();
    }

    @Test
    void add_persistsUserAndReturnsGeneratedId() {
        UserFullResponseDto created = userService.add(request("Ivan", "ivan@mail.ru"));

        assertThat(created.getId()).isNotNull();
        assertThat(created.getName()).isEqualTo("Ivan");
        assertThat(created.getEmail()).isEqualTo("ivan@mail.ru");
        assertThat(userRepository.findById(created.getId()))
                .map(User::getEmail)
                .contains("ivan@mail.ru");
    }

    @Test
    void add_duplicateEmail_throwsEntityAlreadyExistsException() {
        userService.add(request("Ivan", "ivan@mail.ru"));

        assertThrows(EntityAlreadyExistsException.class,
                () -> userService.add(request("Petr", "ivan@mail.ru")));
    }

    @Test
    void getById_existingUser_returnsIt() {
        UserFullResponseDto created = userService.add(request("Ivan", "ivan@mail.ru"));

        UserFullResponseDto found = userService.getById(created.getId());

        assertThat(found.getId()).isEqualTo(created.getId());
        assertThat(found.getName()).isEqualTo("Ivan");
        assertThat(found.getEmail()).isEqualTo("ivan@mail.ru");
    }

    @Test
    void getById_missingUser_throwsEntityNotFoundException() {
        assertThrows(EntityNotFoundException.class, () -> userService.getById(999L));
    }

    @Test
    void getAllByIds_returnsOnlyRequestedUsers() {
        UserFullResponseDto first = userService.add(request("Ivan", "ivan@mail.ru"));
        userService.add(request("Petr", "petr@mail.ru"));
        UserFullResponseDto third = userService.add(request("Anna", "anna@mail.ru"));

        assertThat(userService.getAllByIds(List.of(first.getId(), third.getId())))
                .extracting(UserFullResponseDto::getEmail)
                .containsExactlyInAnyOrder("ivan@mail.ru", "anna@mail.ru");
    }

    @Test
    void getAllByIds_emptyList_returnsEmptyList() {
        userService.add(request("Ivan", "ivan@mail.ru"));

        assertThat(userService.getAllByIds(List.of())).isEmpty();
    }

    @Test
    void getAllByIds_missingId_isIgnored() {
        UserFullResponseDto created = userService.add(request("Ivan", "ivan@mail.ru"));

        assertThat(userService.getAllByIds(List.of(created.getId(), 999L)))
                .extracting(UserFullResponseDto::getId)
                .containsExactly(created.getId());
    }

    @Test
    void getAllWithPagination_returnsRequestedPageOrderedById() {
        for (int i = 1; i <= 5; i++) {
            userService.add(request("User" + i, "user" + i + "@mail.ru"));
        }

        assertThat(userService.getAllWithPagination(1, 2))
                .extracting(UserFullResponseDto::getEmail)
                .containsExactly("user2@mail.ru", "user3@mail.ru");
    }

    @Test
    void getAllWithPagination_offsetBeyondLastPage_returnsEmptyList() {
        userService.add(request("Ivan", "ivan@mail.ru"));

        assertThat(userService.getAllWithPagination(10, 10)).isEmpty();
    }

    @Test
    void remove_existingUser_deletesIt() {
        UserFullResponseDto created = userService.add(request("Ivan", "ivan@mail.ru"));

        userService.remove(created.getId());

        assertThat(userRepository.findById(created.getId())).isEmpty();
    }

    @Test
    void remove_missingUser_throwsEntityNotFoundException() {
        assertThrows(EntityNotFoundException.class, () -> userService.remove(999L));
    }

    private UserCreateRequestDto request(String name, String email) {
        UserCreateRequestDto requestDto = new UserCreateRequestDto();
        requestDto.setName(name);
        requestDto.setEmail(email);
        return requestDto;
    }
}
