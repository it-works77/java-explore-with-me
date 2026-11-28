package ru.practicum.ewm.event;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.category.repository.CategoryRepository;
import ru.practicum.ewm.common.exception.EntityNotFoundException;
import ru.practicum.ewm.common.exception.EntityUpdateForbiddenException;
import ru.practicum.ewm.common.exception.IllegalRequestParamException;
import ru.practicum.ewm.event.dto.CreateEventRequestDto;
import ru.practicum.ewm.event.dto.EventFullResponseDto;
import ru.practicum.ewm.event.dto.EventShortResponseDto;
import ru.practicum.ewm.event.dto.LocationDto;
import ru.practicum.ewm.event.dto.QueryEventAdminRequestDto;
import ru.practicum.ewm.event.dto.QueryEventPublicRequestDto;
import ru.practicum.ewm.event.dto.UpdateEventAdminRequestDto;
import ru.practicum.ewm.event.dto.UpdateEventUserRequestDto;
import ru.practicum.ewm.event.model.AdminStateAction;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventSortType;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.event.model.UserStateAction;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;
import ru.practicum.stats.client.StatsClient;
import ru.practicum.stats.client.exception.StatsClientException;
import ru.practicum.stats.dto.SaveStatsRequestDto;
import ru.practicum.stats.dto.StatsResponseDto;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @MockBean
    private StatsClient statsClient;

    private User initiator;
    private Category category;

    @BeforeEach
    void setUp() {
        clearDatabase();
        initiator = userRepository.save(User.builder().name("Ivan").email("ivan@mail.ru").build());
        category = categoryRepository.save(Category.builder().name("Concert").build());
        when(statsClient.getStats(any())).thenReturn(List.of());
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

        assertThat(eventService.getAllWithPagination(initiator.getId(), 1, 2))
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

        assertThat(eventService.searchPublic(
                QueryEventPublicRequestDto.builder().build(), "/events", "127.0.0.1"))
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
                .build(), "/events", "127.0.0.1")).isEmpty();
    }

    @Test
    void updateByAdmin_publishEvent_setsPublishedOn() {
        EventFullResponseDto created = eventService.add(initiator.getId(),
                request("Concert", category, future(7), true));

        LocalDateTime before = LocalDateTime.now();
        EventFullResponseDto published = eventService.updateByAdmin(created.getId(),
                UpdateEventAdminRequestDto.builder().stateAction(AdminStateAction.PUBLISH_EVENT).build());
        LocalDateTime after = LocalDateTime.now();

        assertThat(published.getState()).isEqualTo(EventState.PUBLISHED);
        assertThat(published.getPublishedOn()).isNotNull().isBetween(before, after);
        assertThat(eventRepository.findById(created.getId()).orElseThrow().getPublishedOn())
                .isNotNull();
    }

    @Test
    void updateByAdmin_rejectPending_leavesPublishedOnNull() {
        EventFullResponseDto created = eventService.add(initiator.getId(),
                request("Concert", category, future(7), true));

        EventFullResponseDto rejected = eventService.updateByAdmin(created.getId(),
                UpdateEventAdminRequestDto.builder().stateAction(AdminStateAction.REJECT_EVENT).build());

        assertThat(rejected.getState()).isEqualTo(EventState.CANCELED);
        assertThat(rejected.getPublishedOn()).isNull();
        assertThat(eventRepository.findById(created.getId()).orElseThrow().getPublishedOn()).isNull();
    }

    @Test
    void updateByAdmin_publishEventWithDateInTwoHours_publishes() {
        Event event = savePendingEvent(LocalDateTime.now().plusHours(2));

        EventFullResponseDto published = eventService.updateByAdmin(event.getId(),
                UpdateEventAdminRequestDto.builder().stateAction(AdminStateAction.PUBLISH_EVENT).build());

        assertThat(published.getState()).isEqualTo(EventState.PUBLISHED);
        assertThat(published.getPublishedOn()).isNotNull();
    }

    @Test
    void updateByAdmin_publishEventWithDateSoon_throwsForbidden() {
        Event event = savePendingEvent(LocalDateTime.now().plusMinutes(30));

        assertThrows(EntityUpdateForbiddenException.class, () -> eventService.updateByAdmin(event.getId(),
                UpdateEventAdminRequestDto.builder().stateAction(AdminStateAction.PUBLISH_EVENT).build()));
        assertThat(eventRepository.findById(event.getId()).orElseThrow().getState())
                .isEqualTo(EventState.PENDING);
    }

    @Test
    void updateByAdmin_rejectEventWithDateSoon_cancelsEvent() {
        Event event = savePendingEvent(LocalDateTime.now().plusMinutes(30));

        EventFullResponseDto rejected = eventService.updateByAdmin(event.getId(),
                UpdateEventAdminRequestDto.builder().stateAction(AdminStateAction.REJECT_EVENT).build());

        assertThat(rejected.getState()).isEqualTo(EventState.CANCELED);
    }

    @Test
    void updateByAdmin_editTitleOfEventWithDateSoon_updatesTitle() {
        Event event = savePendingEvent(LocalDateTime.now().plusMinutes(30));

        EventFullResponseDto updated = eventService.updateByAdmin(event.getId(),
                UpdateEventAdminRequestDto.builder().title("Renamed").build());

        assertThat(updated.getTitle()).isEqualTo("Renamed");
        assertThat(updated.getState()).isEqualTo(EventState.PENDING);
    }

    @Test
    void searchPublic_setsViewsFromStatsAndSavesSingleHitForListEndpoint() {
        EventFullResponseDto published = eventService.add(initiator.getId(),
                request("Published", category, future(8), true));
        publish(published.getId());

        String eventUri = "/events/" + published.getId();
        when(statsClient.getStats(any())).thenReturn(List.of(
                StatsResponseDto.builder()
                        .app("ewm-main-service")
                        .uri(eventUri)
                        .hits(5L)
                        .build()));

        List<EventShortResponseDto> result = eventService.searchPublic(
                QueryEventPublicRequestDto.builder().build(), "/events", "127.0.0.1");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getViews()).isEqualTo(5L);

        ArgumentCaptor<SaveStatsRequestDto> captor = ArgumentCaptor.forClass(SaveStatsRequestDto.class);
        verify(statsClient).saveHit(captor.capture());
        assertThat(captor.getValue().getUri()).isEqualTo("/events");
        assertThat(captor.getValue().getApp()).isEqualTo("ewm-main-service");
    }

    @Test
    void searchPublic_statsUnavailable_returnsEventsWithZeroViews() {
        EventFullResponseDto published = eventService.add(initiator.getId(),
                request("Published", category, future(8), true));
        publish(published.getId());

        when(statsClient.getStats(any())).thenThrow(new StatsClientException("stats down", null));
        doThrow(new StatsClientException("stats down", null)).when(statsClient).saveHit(any());

        List<EventShortResponseDto> result = eventService.searchPublic(
                QueryEventPublicRequestDto.builder().build(), "/events", "127.0.0.1");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getViews()).isZero();
    }

    @Test
    void searchPublic_sortByViews_ordersByViewsDescending() {
        EventFullResponseDto low = eventService.add(initiator.getId(),
                request("Low", category, future(7), true));
        EventFullResponseDto high = eventService.add(initiator.getId(),
                request("High", category, future(8), true));
        publish(low.getId());
        publish(high.getId());

        when(statsClient.getStats(any())).thenReturn(List.of(
                StatsResponseDto.builder().app("ewm-main-service")
                        .uri("/events/" + low.getId()).hits(1L).build(),
                StatsResponseDto.builder().app("ewm-main-service")
                        .uri("/events/" + high.getId()).hits(10L).build()));

        List<EventShortResponseDto> result = eventService.searchPublic(
                QueryEventPublicRequestDto.builder().sort(EventSortType.VIEWS).build(),
                "/events", "127.0.0.1");

        assertThat(result).extracting(EventShortResponseDto::getTitle)
                .containsExactly("High", "Low");
    }

    @Test
    void searchPublic_sortByViews_appliesPaginationAfterSorting() {
        EventFullResponseDto low = eventService.add(initiator.getId(),
                request("Low", category, future(7), true));
        EventFullResponseDto high = eventService.add(initiator.getId(),
                request("High", category, future(8), true));
        publish(low.getId());
        publish(high.getId());

        when(statsClient.getStats(any())).thenReturn(List.of(
                StatsResponseDto.builder().app("ewm-main-service")
                        .uri("/events/" + low.getId()).hits(1L).build(),
                StatsResponseDto.builder().app("ewm-main-service")
                        .uri("/events/" + high.getId()).hits(10L).build()));

        List<EventShortResponseDto> result = eventService.searchPublic(
                QueryEventPublicRequestDto.builder()
                        .sort(EventSortType.VIEWS)
                        .from(0)
                        .size(1)
                        .build(),
                "/events", "127.0.0.1");

        assertThat(result).extracting(EventShortResponseDto::getTitle)
                .containsExactly("High");
    }

    @Test
    void searchPublic_rangeStartAfterRangeEnd_throwsBadRequest() {
        assertThrows(IllegalRequestParamException.class, () -> eventService.searchPublic(
                QueryEventPublicRequestDto.builder()
                        .rangeStart(future(10))
                        .rangeEnd(future(5))
                        .build(),
                "/events", "127.0.0.1"));
    }

    @Test
    void searchPublic_defaultRange_excludesPastEvents() {
        Event past = savePublishedEvent(LocalDateTime.now().minusDays(1));
        Event future = savePublishedEvent(LocalDateTime.now().plusDays(1));

        List<EventShortResponseDto> result = eventService.searchPublic(
                QueryEventPublicRequestDto.builder().build(), "/events", "127.0.0.1");

        assertThat(result).extracting(EventShortResponseDto::getId)
                .contains(future.getId())
                .doesNotContain(past.getId());
    }

    @Test
    void searchPublic_onlyRangeEnd_stillExcludesPastEvents() {
        Event past = savePublishedEvent(LocalDateTime.now().minusDays(1));
        Event future = savePublishedEvent(LocalDateTime.now().plusDays(1));

        List<EventShortResponseDto> result = eventService.searchPublic(
                QueryEventPublicRequestDto.builder()
                        .rangeEnd(LocalDateTime.now().plusDays(10))
                        .build(),
                "/events", "127.0.0.1");

        assertThat(result).extracting(EventShortResponseDto::getId)
                .contains(future.getId())
                .doesNotContain(past.getId());
    }

    @Test
    void searchPublic_withoutPaidFilter_returnsFreeAndPaidEvents() {
        CreateEventRequestDto freeRequest = request("Free", category, future(7), true);
        CreateEventRequestDto paidRequest = request("Paid", category, future(8), true);
        paidRequest.setPaid(true);

        EventFullResponseDto free = eventService.add(initiator.getId(), freeRequest);
        EventFullResponseDto paid = eventService.add(initiator.getId(), paidRequest);
        publish(free.getId());
        publish(paid.getId());

        List<EventShortResponseDto> result = eventService.searchPublic(
                QueryEventPublicRequestDto.builder().build(), "/events", "127.0.0.1");

        assertThat(result).extracting(EventShortResponseDto::getId)
                .contains(free.getId(), paid.getId());
    }

    @Test
    void getPublishedEvent_returnsFullDtoWithViews() {
        EventFullResponseDto published = eventService.add(initiator.getId(),
                request("Published", category, future(8), true));
        publish(published.getId());

        String eventUri = "/events/" + published.getId();
        when(statsClient.getStats(any())).thenReturn(List.of(
                StatsResponseDto.builder()
                        .app("ewm-main-service")
                        .uri(eventUri)
                        .hits(7L)
                        .build()));

        EventFullResponseDto result = eventService.getPublishedEvent(
                published.getId(), eventUri, "127.0.0.1");

        assertThat(result.getDescription()).isEqualTo("Достаточно длинное описание события для теста");
        assertThat(result.getLocation().lat()).isEqualTo(55.75);
        assertThat(result.getState()).isEqualTo(EventState.PUBLISHED);
        assertThat(result.getPublishedOn()).isNotNull();
        assertThat(result.getViews()).isEqualTo(7L);

        ArgumentCaptor<SaveStatsRequestDto> captor = ArgumentCaptor.forClass(SaveStatsRequestDto.class);
        verify(statsClient).saveHit(captor.capture());
        assertThat(captor.getValue().getUri()).isEqualTo(eventUri);
    }

    @Test
    void getPublishedEvent_notPublished_throwsNotFound() {
        EventFullResponseDto pending = eventService.add(initiator.getId(),
                request("Pending", category, future(7), true));

        assertThrows(EntityNotFoundException.class, () -> eventService.getPublishedEvent(
                pending.getId(), "/events/" + pending.getId(), "127.0.0.1"));
    }

    @Test
    void getPublishedEvent_statsUnavailable_returnsZeroViews() {
        EventFullResponseDto published = eventService.add(initiator.getId(),
                request("Published", category, future(8), true));
        publish(published.getId());

        when(statsClient.getStats(any())).thenThrow(new StatsClientException("stats down", null));

        EventFullResponseDto result = eventService.getPublishedEvent(
                published.getId(), "/events/" + published.getId(), "127.0.0.1");

        assertThat(result.getViews()).isZero();
    }

    @Test
    void searchPublic_textWithWildcard_matchesOnlyLiteralPercent() {
        CreateEventRequestDto withPercent = request("Percent", category, future(7), true);
        withPercent.setAnnotation("Скидка 100% на билеты");
        CreateEventRequestDto withoutPercent = request("Regular", category, future(8), true);

        EventFullResponseDto percent = eventService.add(initiator.getId(), withPercent);
        EventFullResponseDto regular = eventService.add(initiator.getId(), withoutPercent);
        publish(percent.getId());
        publish(regular.getId());

        List<EventShortResponseDto> result = eventService.searchPublic(
                QueryEventPublicRequestDto.builder().text("%").build(), "/events", "127.0.0.1");

        assertThat(result).extracting(EventShortResponseDto::getId)
                .contains(percent.getId())
                .doesNotContain(regular.getId());
    }

    @Test
    void searchPublic_paidTrue_returnsOnlyPaidEvents() {
        CreateEventRequestDto freeRequest = request("Free", category, future(7), true);
        CreateEventRequestDto paidRequest = request("Paid", category, future(8), true);
        paidRequest.setPaid(true);

        EventFullResponseDto free = eventService.add(initiator.getId(), freeRequest);
        EventFullResponseDto paid = eventService.add(initiator.getId(), paidRequest);
        publish(free.getId());
        publish(paid.getId());

        List<EventShortResponseDto> result = eventService.searchPublic(
                QueryEventPublicRequestDto.builder().paid(true).build(), "/events", "127.0.0.1");

        assertThat(result).extracting(EventShortResponseDto::getId)
                .containsExactly(paid.getId());
    }

    private void publish(Long eventId) {
        eventService.updateByAdmin(eventId,
                UpdateEventAdminRequestDto.builder().stateAction(AdminStateAction.PUBLISH_EVENT).build());
    }

    private Event savePendingEvent(LocalDateTime eventDate) {
        return eventRepository.save(Event.builder()
                .title("Soon")
                .annotation("Достаточно длинная аннотация события")
                .description("Достаточно длинное описание события для теста")
                .category(category)
                .initiator(initiator)
                .eventDate(eventDate)
                .latitude(55.75)
                .longitude(37.62)
                .requestModeration(true)
                .build());
    }

    private Event savePublishedEvent(LocalDateTime eventDate) {
        return eventRepository.save(Event.builder()
                .title("Published")
                .annotation("Достаточно длинная аннотация события")
                .description("Достаточно длинное описание события для теста")
                .category(category)
                .initiator(initiator)
                .eventDate(eventDate)
                .latitude(55.75)
                .longitude(37.62)
                .requestModeration(true)
                .state(EventState.PUBLISHED)
                .publishedOn(LocalDateTime.now())
                .build());
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
