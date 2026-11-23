package ru.practicum.ewm.event;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.category.repository.CategoryRepository;
import ru.practicum.ewm.common.exception.EntityNotFoundException;
import ru.practicum.ewm.common.exception.EntityUpdateForbiddenException;
import ru.practicum.ewm.event.dto.CreateEventRequestDto;
import ru.practicum.ewm.event.dto.EventFullResponseDto;
import ru.practicum.ewm.event.dto.EventShortResponseDto;
import ru.practicum.ewm.event.dto.LocationDto;
import ru.practicum.ewm.event.dto.QueryEventAdminRequestDto;
import ru.practicum.ewm.event.dto.QueryEventPublicRequestDto;
import ru.practicum.ewm.event.dto.UpdateEventAdminRequestDto;
import ru.practicum.ewm.event.dto.UpdateEventUserRequestDto;
import ru.practicum.ewm.event.model.AdminStateAction;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.event.model.UserStateAction;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class EventServiceImplIntegrationTest {

    @Autowired
    private EventService eventService;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private User initiator;
    private Category category;

    @BeforeEach
    void setUp() {
        clearDatabase();
        initiator = userRepository.save(User.builder().name("Ivan").email("ivan@mail.ru").build());
        category = categoryRepository.save(Category.builder().name("Concert").build());
    }

    @AfterEach
    void tearDown() {
        clearDatabase();
    }

    private void clearDatabase() {
        eventRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void add_persistsEventWithAllFields() {
        CreateEventRequestDto request = request("Concert", category, future(7), false);
        request.setPaid(true);
        request.setParticipantLimit(5);

        EventFullResponseDto created = eventService.add(initiator.getId(), request);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getTitle()).isEqualTo("Concert");
        assertThat(created.getState()).isEqualTo(EventState.PENDING);
        assertThat(created.getInitiator().getId()).isEqualTo(initiator.getId());
        assertThat(created.getCategory().getId()).isEqualTo(category.getId());
        assertThat(created.getLocation().lat()).isEqualTo(55.75);
        assertThat(created.getLocation().lon()).isEqualTo(37.62);
        assertThat(created.isPaid()).isTrue();
        assertThat(created.getParticipantLimit()).isEqualTo(5);
        assertThat(created.isRequestModeration()).isFalse();
        assertThat(created.getCreatedOn()).isNotNull();
        assertThat(eventRepository.findById(created.getId())).isPresent();
    }

    @Test
    void add_eventDateTooSoon_throwsForbidden() {
        CreateEventRequestDto request = request("Concert", category, LocalDateTime.now().plusHours(1), true);

        assertThrows(EntityUpdateForbiddenException.class, () -> eventService.add(initiator.getId(), request));
    }

    @Test
    void add_unknownUser_throwsNotFound() {
        assertThrows(EntityNotFoundException.class,
                () -> eventService.add(999L, request("Concert", category, future(7), true)));
    }

    @Test
    void add_unknownCategory_throwsNotFound() {
        CreateEventRequestDto request = request("Concert", category, future(7), true);
        request.setCategory(999L);

        assertThrows(EntityNotFoundException.class, () -> eventService.add(initiator.getId(), request));
    }

    @Test
    void get_forInitiator_returnsEvent() {
        EventFullResponseDto created = eventService.add(initiator.getId(),
                request("Concert", category, future(7), true));

        assertThat(eventService.get(initiator.getId(), created.getId()).getId()).isEqualTo(created.getId());
    }

    @Test
    void get_forAnotherUser_throwsNotFound() {
        User another = userRepository.save(User.builder().name("Petr").email("petr@mail.ru").build());
        EventFullResponseDto created = eventService.add(initiator.getId(),
                request("Concert", category, future(7), true));

        assertThrows(EntityNotFoundException.class, () -> eventService.get(another.getId(), created.getId()));
    }

    @Test
    void searchPublicForUser_returnsRequestedPage() {
        eventService.add(initiator.getId(), request("E1", category, future(7), true));
        eventService.add(initiator.getId(), request("E2", category, future(8), true));
        eventService.add(initiator.getId(), request("E3", category, future(9), true));

        assertThat(eventService.searchPublic(initiator.getId(), 1, 2))
                .extracting(EventShortResponseDto::getTitle)
                .hasSize(2);
    }

    @Test
    void updateByUser_partialPatchKeepsUntouchedFieldsAndChangesState() {
        CreateEventRequestDto create = request("Concert", category, future(7), false);
        create.setPaid(true);
        create.setParticipantLimit(10);
        EventFullResponseDto created = eventService.add(initiator.getId(), create);

        EventFullResponseDto updated = eventService.update(initiator.getId(), created.getId(),
                UpdateEventUserRequestDto.builder()
                        .title("Renamed")
                        .stateAction(UserStateAction.CANCEL_REVIEW)
                        .build());

        assertThat(updated.getTitle()).isEqualTo("Renamed");
        assertThat(updated.getState()).isEqualTo(EventState.CANCELED);
        assertThat(updated.isPaid()).isTrue();
        assertThat(updated.getParticipantLimit()).isEqualTo(10);
        assertThat(updated.isRequestModeration()).isFalse();
    }

    @Test
    void updateByUser_publishedEvent_throwsForbidden() {
        EventFullResponseDto created = eventService.add(initiator.getId(),
                request("Concert", category, future(7), true));
        publish(created.getId());

        assertThrows(EntityUpdateForbiddenException.class, () -> eventService.update(initiator.getId(),
                created.getId(), UpdateEventUserRequestDto.builder().title("Renamed").build()));
    }

    @Test
    void updateByAdmin_publishAndReject() {
        EventFullResponseDto created = eventService.add(initiator.getId(),
                request("Concert", category, future(7), true));

        EventFullResponseDto published = eventService.updateByAdmin(created.getId(),
                UpdateEventAdminRequestDto.builder().stateAction(AdminStateAction.PUBLISH_EVENT).build());
        assertThat(published.getState()).isEqualTo(EventState.PUBLISHED);

        assertThrows(EntityUpdateForbiddenException.class, () -> eventService.updateByAdmin(created.getId(),
                UpdateEventAdminRequestDto.builder().stateAction(AdminStateAction.PUBLISH_EVENT).build()));

        assertThrows(EntityUpdateForbiddenException.class, () -> eventService.updateByAdmin(created.getId(),
                UpdateEventAdminRequestDto.builder().stateAction(AdminStateAction.REJECT_EVENT).build()));
    }

    @Test
    void updateByAdmin_rejectPending_cancelsEvent() {
        EventFullResponseDto created = eventService.add(initiator.getId(),
                request("Concert", category, future(7), true));

        EventFullResponseDto rejected = eventService.updateByAdmin(created.getId(),
                UpdateEventAdminRequestDto.builder().stateAction(AdminStateAction.REJECT_EVENT).build());
        assertThat(rejected.getState()).isEqualTo(EventState.CANCELED);
    }

    @Test
    void updateByAdmin_partialPatchKeepsUntouchedFields() {
        CreateEventRequestDto create = request("Concert", category, future(7), false);
        create.setPaid(true);
        create.setParticipantLimit(10);
        EventFullResponseDto created = eventService.add(initiator.getId(), create);

        EventFullResponseDto updated = eventService.updateByAdmin(created.getId(),
                UpdateEventAdminRequestDto.builder().title("Renamed").build());

        assertThat(updated.getTitle()).isEqualTo("Renamed");
        assertThat(updated.isPaid()).isTrue();
        assertThat(updated.getParticipantLimit()).isEqualTo(10);
        assertThat(updated.isRequestModeration()).isFalse();
    }

    @Test
    void searchAdmin_returnsListWithMappedFields() {
        EventFullResponseDto created = eventService.add(initiator.getId(),
                request("Concert", category, future(7), true));

        List<EventFullResponseDto> found = eventService.searchAdmin(QueryEventAdminRequestDto.builder()
                .users(List.of(initiator.getId()))
                .categories(List.of(category.getId()))
                .states(List.of(EventState.PENDING))
                .build());

        assertThat(found).extracting(EventFullResponseDto::getId).contains(created.getId());
        assertThat(found.get(0).getCategory().getName()).isEqualTo("Concert");
        assertThat(found.get(0).getInitiator().getName()).isEqualTo("Ivan");
    }

    @Test
    void searchPublic_returnsOnlyPublishedEvents() {
        EventFullResponseDto pending = eventService.add(initiator.getId(),
                request("Pending", category, future(7), true));
        EventFullResponseDto published = eventService.add(initiator.getId(),
                request("Published", category, future(8), true));
        publish(published.getId());

        assertThat(eventService.searchPublic(QueryEventPublicRequestDto.builder().build()))
                .extracting(EventShortResponseDto::getId)
                .containsExactly(published.getId())
                .doesNotContain(pending.getId());
    }

    @Test
    void searchPublic_withFutureRangeStart_returnsEmptyList() {
        EventFullResponseDto published = eventService.add(initiator.getId(),
                request("Published", category, future(8), true));
        publish(published.getId());

        assertThat(eventService.searchPublic(QueryEventPublicRequestDto.builder()
                .rangeStart(future(30))
                .build())).isEmpty();
    }

    @Test
    void getPublic_unpublishedEvent_throwsNotFound() {
        EventFullResponseDto created = eventService.add(initiator.getId(),
                request("Concert", category, future(7), true));

        assertThrows(EntityNotFoundException.class, () -> eventService.get(created.getId()));
    }

    private void publish(Long eventId) {
        eventService.updateByAdmin(eventId,
                UpdateEventAdminRequestDto.builder().stateAction(AdminStateAction.PUBLISH_EVENT).build());
    }

    private CreateEventRequestDto request(String title, Category eventCategory, LocalDateTime date,
                                          boolean requestModeration) {
        return CreateEventRequestDto.builder()
                .title(title)
                .annotation("Достаточно длинная аннотация события")
                .description("Достаточно длинное описание события для теста")
                .category(eventCategory.getId())
                .eventDate(date)
                .location(new LocationDto(55.75, 37.62))
                .requestModeration(requestModeration)
                .build();
    }

    private LocalDateTime future(int days) {
        return LocalDateTime.now().plusDays(days);
    }
}
