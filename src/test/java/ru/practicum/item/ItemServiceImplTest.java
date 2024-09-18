package ru.practicum.item;

import com.querydsl.core.types.dsl.BooleanExpression;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import ru.practicum.common.AccessException;
import ru.practicum.common.NotFoundException;
import ru.practicum.item.dto.AddItemRequest;
import ru.practicum.item.dto.GetItemRequest;
import ru.practicum.item.dto.ItemDto;
import ru.practicum.item.dto.ModifyItemRequest;
import ru.practicum.user.User;
import ru.practicum.user.UserRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemServiceImplTest extends ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UrlMetaDataRetriever urlMetaDataRetriever;

    @InjectMocks
    ItemServiceImpl itemService;

    @Test
    void addNewItem_shouldReturnAddedItem_whenUserExists() {
        when(userRepository.findById(anyLong()))
                .thenAnswer(inv -> {
                    Long userId = inv.getArgument(0, Long.class);
                    User user = makeDefaultUser();
                    user.setId(userId);
                    return Optional.of(user);
                });
        when(urlMetaDataRetriever.retrieve(anyString()))
                .thenAnswer(inv -> {
                    String url = inv.getArgument(0, String.class);
                    return makeUrlMetaData(url, url, "text/html", "test", false, false, hoursFromNow(2));
                });
        when(itemRepository.findByUserAndResolvedUrl(any(User.class), anyString()))
                .thenReturn(Optional.empty());
        when(itemRepository.save(any(Item.class)))
                .then(inv -> {
                    Item item = inv.getArgument(0, Item.class);
                    item.setId(1L);
                    return item;
                });

        var request = AddItemRequest.builder()
                .url("https://google.com")
                .build();
        ItemDto addedNewItem = itemService.addNewItem(1L, request);
        assertThat(1L, equalTo(addedNewItem.getId()));
        assertThat("https://google.com", equalTo(addedNewItem.getNormalUrl()));

        verify(userRepository).findById(anyLong());
        verify(itemRepository).save(any());
        verify(urlMetaDataRetriever).retrieve(anyString());

        verifyNoMoreInteractions(userRepository);
        verifyNoMoreInteractions(itemRepository);
        verifyNoMoreInteractions(urlMetaDataRetriever);
    }

    @Test
    void addNewItem_shouldThrowNotFoundException_whenUserDoesNotExist() {
        when(userRepository.findById(anyLong()))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> {
            var request = AddItemRequest.builder().build();
            itemService.addNewItem(999L, request);
        });
        verify(userRepository).findById(anyLong());
        verifyNoInteractions(itemRepository);
    }

    @Test
    void addNewItem_shouldSupplementTagsOfItem_whenItemByResolvedUrlAlreadyExists() {
        // given
        var request = makeItemRequest("https://yandex.ru", Set.of("spring"));

        var userId = 1L;
        var defaultUser = makeDefaultUserWithId(userId);

        var itemId = 1L;
        var defaultItem = makeDefaultItemWithId(defaultUser, itemId);

        var urlMetadata = extractUrlMetaDataFromItem(defaultItem);

        when(userRepository.findById(anyLong()))
                .thenReturn(Optional.of(defaultUser));
        when(itemRepository.findByUserAndResolvedUrl(any(User.class), anyString()))
                .thenReturn(Optional.of(defaultItem));
        when(urlMetaDataRetriever.retrieve(anyString()))
                .thenReturn(urlMetadata);
        when(itemRepository.save(any(Item.class)))
                .thenReturn(defaultItem);

        // when
        ItemDto addedNewItem = itemService.addNewItem(userId, request);

        // then
        assertThat(addedNewItem.getTags(), containsInAnyOrder("yandex", "practicum", "spring"));

        verify(userRepository).findById(1L);
        verifyNoMoreInteractions(userRepository);

        verify(urlMetaDataRetriever).retrieve(request.getUrl());
        verifyNoMoreInteractions(urlMetaDataRetriever);

        verify(itemRepository).findByUserAndResolvedUrl(defaultUser, urlMetadata.getResolvedUrl());
        verifyNoMoreInteractions(itemRepository);

        verify(itemRepository).save(any(Item.class));
        verifyNoMoreInteractions(itemRepository);
    }

    @Test
    void getItems_shouldReturnThreeItems_whenFilterIsNotStrong() {
        // given
        var userId = 1L;
        var defaultUser = makeDefaultUserWithId(userId);

        var getItemRequest = new GetItemRequest(userId, GetItemRequest.State.ALL, GetItemRequest.ContentType.ALL, GetItemRequest.Sort.NEWEST,
                5, List.of());

        var itemDto1 = makeItem(defaultUser, "https://bit.ly/3vRVvO0", "https://practicum.yandex.ru/java-developer/", "text/html", "Курс «Java-разработчик» с нуля: онлайн-обучение Java-программированию для начинающих — Яндекс Практикум", true,
                true, daysFromNow(-730), true, Set.of("yandex", "practicum"));
        var itemDto2 = makeItem(defaultUser, "https://some-video-url", "https://some-resolved-url-video.com", "text/html", "some article title", false,
                false, daysFromNow(-365), true,
                Set.of("video"));
        var itemDto3 = makeItem(defaultUser, "https://some-image-url", "https://some-resolved-url-image.com", "img", "some youtube name", true,
                true, daysFromNow(-300), false,
                Set.of("image"));
        List<Item> sourceItems = List.of(itemDto3, itemDto2, itemDto1);

        when(itemRepository.findAll(any(BooleanExpression.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(sourceItems));

        // when
        List<ItemDto> targetItems = itemService.getItems(getItemRequest);

        // then
        assertThat(targetItems.size(), equalTo(3));
        assertThat(targetItems.getFirst(), hasProperty("title", equalTo("some youtube name")));
    }

    @Test
    void getItems_shouldReturnTwoItems_whenStateIsUnreadAndContentTypeIsVideoAndSortIsNewest() {
        // given
        var userId = 1L;
        var defaultUser = makeDefaultUserWithId(userId);

        var getItemRequest = new GetItemRequest(userId, GetItemRequest.State.UNREAD, GetItemRequest.ContentType.ARTICLE, GetItemRequest.Sort.NEWEST,
                5, List.of());

        var itemDto2 = makeItem(defaultUser, "https://some-video-url", "https://some-resolved-url-video.com", "text/html", "some youtube title", false,
                false, daysFromNow(-365), true,
                Set.of("video"));
        var itemDto3 = makeItem(defaultUser, "https://some-image-url", "https://some-resolved-url-image.com", "img", "some article name", true,
                true, daysFromNow(-300), false,
                Set.of("image"));
        List<Item> sourceItems = List.of(itemDto2, itemDto3);

        when(itemRepository.findAll(any(BooleanExpression.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(sourceItems));

        // when
        List<ItemDto> targetItems = itemService.getItems(getItemRequest);

        // then
        assertThat(targetItems.size(), equalTo(2));
        assertThat(targetItems.getFirst(), hasProperty("title", equalTo("some youtube title")));
    }

    @Test
    void edit_shouldNotThrowAnyException_whenReplaceTagsIsTrueAndUnreadIsTrue() {
        // given
        var userId = 1L;
        var defaultUser = makeDefaultUserWithId(userId);
        var itemId = 5L;
        var defaultItem = makeDefaultItemWithId(defaultUser, itemId);

        var request = new ModifyItemRequest();
        request.setId(itemId);
        request.setReplaceTags(true);
        request.setTags(Set.of("replacement_for_yandex_tag"));
        request.setUnread(true);

        when(itemRepository.findById(anyLong()))
                .thenReturn(Optional.of(defaultItem));


        // when
        Executable editItem = () -> itemService.edit(userId, request);

        // then
        assertDoesNotThrow(editItem);
    }

    @Test
    void editItem_shouldThrowAccessException_whenUserIdIsWrong() {
        // given
        var userId = 1L;
        var defaultUser = makeDefaultUserWithId(userId);
        var wrongUserId = userId + 1L;
        var itemId = 5L;
        var defaultItem = makeDefaultItemWithId(defaultUser, itemId);

        var request = new ModifyItemRequest();
        request.setId(itemId);
        request.setReplaceTags(true);
        request.setTags(Set.of("replacement_for_yandex_tag"));
        request.setUnread(true);

        when(itemRepository.findById(anyLong()))
                .thenReturn(Optional.of(defaultItem));

        // when
        Executable editItem = () -> itemService.edit(wrongUserId, request);

        // then
        AccessException accessException = assertThrows(AccessException.class, editItem);
        assertThat(accessException.getHttpStatus(), equalTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void editItem_shouldThrowNotFoundException_whenUserIdIsWrong() {
        // given
        var userId = 1L;
        var itemId = 5L;

        var request = new ModifyItemRequest();
        request.setId(itemId);
        request.setReplaceTags(true);
        request.setTags(Set.of("replacement_for_yandex_tag"));
        request.setUnread(true);

        when(itemRepository.findById(anyLong()))
                .thenReturn(Optional.empty());

        // when
        Executable editItem = () -> itemService.edit(userId, request);

        // then
        var notFoundException = assertThrows(NotFoundException.class, editItem);
        assertThat(notFoundException.getHttpStatus(), equalTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void deleteItem_shouldThrowNoResultException_whenGettingDeletedItem() {
        // given
        var userId = 1L;
        var itemId = 2L;

        // when
        Executable deleteItem = () -> itemService.deleteItem(userId, itemId);

        // then
        assertDoesNotThrow(deleteItem);
    }
}