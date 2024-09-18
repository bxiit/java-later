package ru.practicum.item;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.common.AccessException;
import ru.practicum.common.ItemRetrieverException;
import ru.practicum.common.NotFoundException;
import ru.practicum.config.AppConfig;
import ru.practicum.config.PersistenceConfig;
import ru.practicum.item.dto.AddItemRequest;
import ru.practicum.item.dto.GetItemRequest;
import ru.practicum.item.dto.ItemDto;
import ru.practicum.item.dto.ModifyItemRequest;
import ru.practicum.user.User;
import ru.practicum.user.UserState;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Transactional
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@SpringJUnitConfig({AppConfig.class, PersistenceConfig.class,
        ItemServiceImpl.class, UrlMetaDataRetrieverImpl.class,
        ItemMapper.class})
@TestPropertySource(properties = {
        "jdbc.url=jdbc:postgresql://localhost:5432/test",
        "hibernate.hbm2ddl.auto=update"
})
public class ItemServiceIntegrationTest extends ItemServiceTest {

    private final EntityManager em;
    private final ItemService itemService;

    @Test
    void addNewItem_shouldReturnAddedItem_whenEverythingIsOK() {
        User defaultUser = makeDefaultUser();
        em.persist(defaultUser);
        AddItemRequest request = makeItemRequest("https://github.com/", Set.of());
        itemService.addNewItem(defaultUser.getId(), request);

        TypedQuery<Item> query = em.createQuery("select it from Item as it where it.url = :url", Item.class);
        Item item = query.setParameter("url", request.getUrl())
                .getSingleResult();

        assertThat(item.getId(), notNullValue());
        assertThat(item.getUser().getId(), equalTo(defaultUser.getId()));
        assertThat(item.getUser().getEmail(), equalTo("email"));
        assertThat(item.getUser().getState(), equalTo(UserState.ACTIVE));
        assertThat(item.getUrl(), equalTo("https://github.com/"));
        assertThat(item.getHasVideo(), equalTo(true));
        assertThat(item.getHasImage(), equalTo(true));
        assertThat(item.getUnread(), equalTo(true));
    }

    @Test
    void addNewItem_shouldReturnAddedItem_whenEverythingIsOKAndContentTypeIsImage() {
        User defaultUser = makeDefaultUser();
        em.persist(defaultUser);
        AddItemRequest request = makeItemRequest("https://httpbin.org/image", Set.of());
        itemService.addNewItem(defaultUser.getId(), request);

        TypedQuery<Item> query = em.createQuery("select it from Item as it where it.url = :url", Item.class);
        Item item = query.setParameter("url", request.getUrl())
                .getSingleResult();

        assertThat(item.getId(), notNullValue());
        assertThat(item.getUser().getId(), equalTo(defaultUser.getId()));
        assertThat(item.getUser().getEmail(), equalTo("email"));
        assertThat(item.getUser().getState(), equalTo(UserState.ACTIVE));
        assertThat(item.getUrl(), equalTo("https://httpbin.org/image"));
        assertThat(item.getHasVideo(), equalTo(false));
        assertThat(item.getHasImage(), equalTo(true));
        assertThat(item.getUnread(), equalTo(true));
    }

    @Test
    void addNewItem_shouldThrowItemRetrieverException_whenUnauthorized() {
        // given
        User defaultUser = makeDefaultUser();
        em.persist(defaultUser);
        AddItemRequest request = makeItemRequest("https://httpbin.org/status/401", Set.of());

        // when
        Executable addNewItem = () -> itemService.addNewItem(defaultUser.getId(), request);

        // then
        ItemRetrieverException itemRetrieverException = assertThrows(ItemRetrieverException.class, addNewItem);
        assertThat(itemRetrieverException.getMessage(), equalTo("There is no access to the resource at the specified URL: https://httpbin.org/status/401"));
    }

    @Test
    void addNewItem_shouldThrowItemRetrieverException_whenUrlIsMalformed() {
        // given
        User defaultUser = makeDefaultUser();
        em.persist(defaultUser);
        AddItemRequest request = makeItemRequest("ht^tp://invalid_uri.com", Set.of());

        // when
        Executable addNewItem = () -> itemService.addNewItem(defaultUser.getId(), request);

        // then
        ItemRetrieverException itemRetrieverException = assertThrows(ItemRetrieverException.class, addNewItem);
        assertThat(itemRetrieverException.getMessage(), equalTo("The URL is malformed: ht^tp://invalid_uri.com"));
    }

    @Test
    void addNewItem_shouldThrowItemRetrieverException_whenResponseStatusIs400() {
        // given
        User defaultUser = makeDefaultUser();
        em.persist(defaultUser);
        AddItemRequest request = makeItemRequest("https://httpbin.org/status/400", Set.of());

        // when
        Executable addNewItem = () -> itemService.addNewItem(defaultUser.getId(), request);

        // then
        ItemRetrieverException itemRetrieverException = assertThrows(ItemRetrieverException.class, addNewItem);
        assertThat(
                itemRetrieverException.getMessage(),
                equalTo("Cannot get the data on the item because the server returned an error.Response status: 400 BAD_REQUEST")
        );
    }

    @Test
    void addNewItem_shouldThrowItemRetrieverException_whenResponseStatusIs600() {
        // given
        User defaultUser = makeDefaultUser();
        em.persist(defaultUser);
        AddItemRequest request = makeItemRequest("https://httpbin.org/status/600", Set.of());

        // when
        Executable addNewItem = () -> itemService.addNewItem(defaultUser.getId(), request);

        // then
        ItemRetrieverException itemRetrieverException = assertThrows(ItemRetrieverException.class, addNewItem);
        assertThat(
                itemRetrieverException.getMessage(),
                equalTo("The server returned an unknown status code: 600")
        );
    }

    @Test
    void addNewItem_shouldThrowItemRetrieverException_whenResponseContentTypeIsNotSupported() {
        // given
        User defaultUser = makeDefaultUser();
        em.persist(defaultUser);
        AddItemRequest request = makeItemRequest("https://httpbin.org/json", Set.of());

        // when
        Executable addNewItem = () -> itemService.addNewItem(defaultUser.getId(), request);

        // then
        ItemRetrieverException itemRetrieverException = assertThrows(ItemRetrieverException.class, addNewItem);
        assertThat(
                itemRetrieverException.getMessage(),
                equalTo("The content type [ application/json ] at the specified URL is not supported.")
        );
    }

    @Test
    void addNewItem_shouldThrowNotFoundException_whenUserIdDoesExist() {
        // given
        Long notExistingUserId = 76L;
        AddItemRequest request = makeItemRequest("https://bit.ly/3vRVvO0", Set.of());

        NotFoundException notFoundException = assertThrows(NotFoundException.class, () -> itemService.addNewItem(notExistingUserId, request));
        assertEquals(HttpStatus.NOT_FOUND, notFoundException.getHttpStatus());
    }

    @Test
    void addNewItem_shouldReturnTagAddedToExistingItem_whenResolvedIdIsAlreadyExists() {
        // given
        User defaultUser = makeDefaultUser();
        em.persist(defaultUser);
        String sameUrl = "https://stackoverflow.com/questions/40268446/junit-5-how-to-assert-an-exception-is-thrown";
        AddItemRequest oldRequest = makeItemRequest(sameUrl, Set.of("yandex"));
        ItemDto oldItem = itemService.addNewItem(defaultUser.getId(), oldRequest);
        AddItemRequest newRequest = makeItemRequest(sameUrl, Set.of("practicum"));

        // when
        ItemDto newItem = itemService.addNewItem(defaultUser.getId(), newRequest);

        // then
        assertThat(newItem.getTags(), containsInAnyOrder("yandex", "practicum"));
    }

    @Test
    void getItems_shouldReturnAllSavedItems_whenFilterAreNotStrong() {

        // given
        final User defaultUser = makeDefaultUser();
        em.persist(defaultUser);

        List<ItemDto> sourceItems = List.of(
                makeItemDto(defaultUser, true, Set.of("yandex", "practicum"), new UrlMetaDataRetrieverImpl.UrlMetadataImpl("https://bit.ly/3vRVvO0", "https://practicum.yandex.ru/java-developer/", "text",
                        "Курс «Java-разработчик» с нуля: онлайн-обучение Java-программированию для начинающих — Яндекс Практикум", true, true, daysFromNow(730))),
                makeItemDto(defaultUser, false, Set.of("video"), new UrlMetaDataRetrieverImpl.UrlMetadataImpl("https://some-video-url", "https://some-resolved-url-video.com", "video",
                        "some title", false, true,
                        daysFromNow(365))),
                makeItemDto(defaultUser, true, Set.of("image"), new UrlMetaDataRetrieverImpl.UrlMetadataImpl("https://some-image-url", "https://some-resolved-url-image.com", "image",
                        "some title", true, false,
                        daysFromNow(300)))
        );

        for (ItemDto sourceItem : sourceItems) {
            Item item = ItemMapper.mapToNewItem(extractUrlMetaDataFromItemDto(sourceItem),
                    defaultUser, sourceItem.getTags());
            em.persist(item);
        }
        em.flush();

        // when
        List<ItemDto> resultItems = itemService.getItems(new GetItemRequest(
                defaultUser.getId(),
                GetItemRequest.State.ALL,
                GetItemRequest.ContentType.ALL,
                GetItemRequest.Sort.NEWEST,
                5,
                emptyList()
        ));

        // then
        assertThat(resultItems.size(), equalTo(3));

        assertThat(resultItems, hasItem(allOf(
                        hasProperty("id", notNullValue()),
                        hasProperty("unread"),
                        hasProperty("title"),
                        hasProperty("normalUrl"),
                        hasProperty("resolvedUrl"),
                        hasProperty("mimeType"),
                        hasProperty("hasImage"),
                        hasProperty("hasVideo"),
                        hasProperty("dateResolved"),
                        hasProperty("tags")
                )
        ));
    }

    @Test
    void getItems_shouldReturnOnlyOneItem_whenContentTypeIsTextAndUnreadIsTrue() {
        // given
        final User defaultUser = makeDefaultUser();
        em.persist(defaultUser);

        List<Item> sourceItems = List.of(
                makeItem(defaultUser, "https://bit.ly/3vRVvO0", "https://practicum.yandex.ru/java-developer/", "text/html",
                        "Курс «Java-разработчик» с нуля: онлайн-обучение Java-программированию для начинающих — Яндекс Практикум", true, true,
                        daysFromNow(-730), true, Set.of("yandex", "practicum")),
                makeItem(defaultUser, "https://some-video-url", "https://some-resolved-url-video.com", "video",
                        "some title", false, true,
                        daysFromNow(-365), false, Set.of("video")),
                makeItem(defaultUser, "https://some-image-url", "https://some-resolved-url-image.com", "image",
                        "some title", true, false,
                        daysFromNow(-300), true, Set.of("image")),
                makeItem(defaultUser, "https://some-notion-url", "https://some-resolved-url-notion.com", "text/html",
                        "some note title", false, false,
                        daysFromNow(-15), false, Set.of("notion", "notes")),
                makeItem(defaultUser, "https://some-pachka-url", "https://some-resolved-url-pachka.com", "image",
                        "some chat title", false, false,
                        daysFromNow(-1), true, Set.of("pachka", "messenger"))
        );

        for (Item sourceItem : sourceItems) {
            em.persist(sourceItem);
        }

        GetItemRequest getItemRequest = new GetItemRequest(
                defaultUser.getId(),
                GetItemRequest.State.UNREAD,
                GetItemRequest.ContentType.ARTICLE,
                GetItemRequest.Sort.NEWEST,
                5,
                emptyList()
        );

        // when
        List<ItemDto> targetItems = itemService.getItems(getItemRequest);

        // then
        assertThat(targetItems.size(), equalTo(1));
        assertThat(targetItems.getFirst(), hasProperty("tags", containsInAnyOrder("yandex", "practicum")));
        assertThat(targetItems, hasItem(allOf(
                hasProperty("id", notNullValue()),
                hasProperty("normalUrl", equalTo("https://bit.ly/3vRVvO0")),
                hasProperty("resolvedUrl", equalTo("https://practicum.yandex.ru/java-developer/")),
                hasProperty("mimeType", equalTo("text/html")),
                hasProperty("title", equalTo("Курс «Java-разработчик» с нуля: онлайн-обучение Java-программированию для начинающих — Яндекс Практикум")),
                hasProperty("hasImage", equalTo(true)),
                hasProperty("hasVideo", equalTo(true)),
                hasProperty("unread", equalTo(true)),
                hasProperty("dateResolved", notNullValue())
        )));
    }

    @Test
    void edit_shouldReturnUpdatedItemWithReplacedTags_whenReplaceTagsIsTrue() {

        // given
        User defaultUser = makeDefaultUser();
        em.persist(defaultUser);

        Item defaultItem = makeDefaultItem(defaultUser);
        em.persist(defaultItem);
        Set<String> oldTags = new HashSet<>(defaultItem.getTags());

        // when
        var editRequest = ModifyItemRequest.of(defaultItem.getId(), false,
                new HashSet<>(Set.of("shuk", "laki")), true);
        itemService.edit(defaultUser.getId(), editRequest);

        // then
        Item editedItem = getEntity(defaultItem.getId(), Item.class);
        assertThat(editedItem.getUnread(), equalTo(false));
        assertThat(editedItem.getTags(), equalTo(Set.of("shuk", "laki")));
        assertThat(oldTags, not(equalTo(editedItem.getTags())));
    }

    @Test
    void edit_shouldThrowForbiddenException_whenUserIdIsWrong() {
        // given
        User defaultUser = makeDefaultUser();
        em.persist(defaultUser);

        Item defaultItem = makeDefaultItem(defaultUser);
        em.persist(defaultItem);
        long wrongUserId = defaultUser.getId() + 1;
        ModifyItemRequest editRequest = new ModifyItemRequest();
        editRequest.setId(defaultItem.getId());
        editRequest.setUnread(false);
        editRequest.setReplaceTags(true);
        editRequest.setTags(Set.of("shuk", "laki"));

        // when
        Executable editItem = () -> itemService.edit(wrongUserId, editRequest);

        // then
        AccessException accessException = assertThrows(AccessException.class, editItem);
        assertEquals(HttpStatus.FORBIDDEN, accessException.getHttpStatus());
    }

    @Test
    void edit_shouldReturnUpdatedItemWithAddedTags_whenReplaceTagsIsFalse() {

        // given
        User defaultUser = makeDefaultUser();
        em.persist(defaultUser);

        Item defaultItem = makeDefaultItem(defaultUser);
        em.persist(defaultItem);
        Set<String> oldTags = new HashSet<>(defaultItem.getTags());

        // when
        ModifyItemRequest editRequest = new ModifyItemRequest();
        editRequest.setId(defaultItem.getId());
        editRequest.setUnread(false);
        editRequest.setReplaceTags(false);
        editRequest.setTags(Set.of("shuk", "laki"));
        itemService.edit(defaultUser.getId(), editRequest);

        // then
        Item editedItem = getEntity(defaultItem.getId(), Item.class);
        assertThat(editedItem.getUnread(), equalTo(false));

        oldTags.addAll(editedItem.getTags());
        assertThat(editedItem.getTags(), equalTo(oldTags));
    }

    @Test
    void deleteItem_shouldThrowNoResultException_whenGettingDeletedItem() {
        // given
        User defaultUser = makeDefaultUser();
        em.persist(defaultUser);

        Item defaultItem = makeDefaultItem(defaultUser);
        em.persist(defaultItem);

        // when
        itemService.deleteItem(defaultUser.getId(), defaultItem.getId());

        // then
        assertThrows(
                NoResultException.class,
                () -> getEntity(defaultItem.getId(), Item.class));
    }

    private <T> T getEntity(long id, Class<T> entityClass) {
        String sqlQuery = "select en from %s en where en.id = :id".formatted(entityClass.getSimpleName());
        TypedQuery<T> query = em.createQuery(sqlQuery, entityClass);
        return query
                .setParameter("id", id)
                .getSingleResult();
    }
}
