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
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.common.AccessException;
import ru.practicum.common.NotFoundException;
import ru.practicum.config.AppConfig;
import ru.practicum.config.PersistenceConfig;
import ru.practicum.item.dto.AddItemRequest;
import ru.practicum.item.dto.GetItemRequest;
import ru.practicum.item.dto.ItemDto;
import ru.practicum.item.dto.ModifyItemRequest;
import ru.practicum.user.User;
import ru.practicum.user.UserState;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.LinkedHashSet;
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
public class ItemServiceIntegrationTest {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter
            .ofPattern("yyyy.MM.dd HH:mm:ss")
            .withZone(ZoneOffset.UTC);

    private final EntityManager em;
    private final ItemService itemService;

    @Test
    void addNewItem_shouldReturnAddedItemFromDB_whenEverythingIsOK() {
        User defaultUser = makeDefaultUser();
        em.persist(defaultUser);
        AddItemRequest request = makeItemRequest("https://bit.ly/3vRVvO0", Set.of());
        itemService.addNewItem(defaultUser.getId(), request);

        TypedQuery<Item> query = em.createQuery("select it from Item as it where it.url = :url", Item.class);
        Item item = query.setParameter("url", request.getUrl())
                .getSingleResult();

        assertThat(item.getId(), notNullValue());
        assertThat(item.getUser().getId(), equalTo(defaultUser.getId()));
        assertThat(item.getUser().getEmail(), equalTo("email"));
        assertThat(item.getUser().getState(), equalTo(UserState.ACTIVE));
        assertThat(item.getUrl(), equalTo("https://bit.ly/3vRVvO0"));
        assertThat(item.getHasImage(), equalTo(true));
        assertThat(item.getUnread(), equalTo(true));
    }

    @Test
    void addNewItem_shouldThrowNotFoundException_whenUserIdDoesExist() {
        // given
        Long notExistingUserId = 76L;
        AddItemRequest request = makeItemRequest("https://bit.ly/3vRVvO0", Set.of());

        NotFoundException notFoundException = assertThrows(NotFoundException.class, () -> itemService.addNewItem(notExistingUserId, request));
        assertEquals(HttpStatus.NOT_FOUND, notFoundException.getHttpStatus());
        assertEquals("Пользователь не найден", notFoundException.getMessage());
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
    void getItems() {
        // given
        final User defaultUser = makeDefaultUser();
        em.persist(defaultUser);

        List<Item> items = List.of(
                makeItem(defaultUser, "https://bit.ly/3vRVvO0", "https://practicum.yandex.ru/java-developer/", "text",
                        "Курс «Java-разработчик» с нуля: онлайн-обучение Java-программированию для начинающих — Яндекс Практикум", true, true,
                        daysFromNow(-730), true, Set.of("yandex", "practicum")),
                makeItem(defaultUser, "https://some-video-url", "https://some-resolved-url-video.com", "video",
                        "some title", false, true,
                        daysFromNow(-365), false, Set.of("video")),
                makeItem(defaultUser, "https://some-image-url", "https://some-resolved-url-image.com", "image",
                        "some title", true, false,
                        daysFromNow(-300), true, Set.of("image")),
                makeItem(defaultUser, "https://some-notion-url", "https://some-resolved-url-notion.com", "text",
                        "some note title", false, false,
                        daysFromNow(-15), false, Set.of("notion", "notes")),
                makeItem(defaultUser, "https://some-pachka-url", "https://some-resolved-url-pachka.com", "image",
                        "some chat title", false, false,
                        daysFromNow(-1), false, Set.of("pachka", "messenger"))
        );

        for (Item item : items) {
            em.persist(item);
        }

        List<ItemDto> targetItems = itemService.getItems(new GetItemRequest(
                defaultUser.getId(),
                GetItemRequest.State.READ,
                GetItemRequest.ContentType.ARTICLE,
                GetItemRequest.Sort.NEWEST,
                5,
                emptyList()
        ));

        for (ItemDto targetItem : targetItems) {
            Item item = ItemMapper.mapToNewItem(
                    extractUrlMetaDataFromItemDto(targetItem),
                    defaultUser,
                    targetItem.getTags()
            );
            em.persist(item);
        }
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
        ModifyItemRequest editRequest = new ModifyItemRequest();
        editRequest.setId(defaultItem.getId());
        editRequest.setUnread(false);
        editRequest.setReplaceTags(true);
        editRequest.setTags(Set.of("shuk", "laki"));
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
    @Sql({"/db/sql/users.sql", "/db/sql/items.sql"})
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

    private UrlMetaDataRetrieverImpl.UrlMetadataImpl extractUrlMetaDataFromItemDto(ItemDto itemDto) {
        return new UrlMetaDataRetrieverImpl.UrlMetadataImpl(itemDto.getNormalUrl(), itemDto.getResolvedUrl(), itemDto.getMimeType(), itemDto.getTitle(), itemDto.getHasImage(), itemDto.getHasVideo(), stringToInstant(itemDto.getDateResolved()));
    }

    private UrlMetaDataRetrieverImpl.UrlMetadataImpl extractUrlMetaDataFromItem(Item item) {
        return new UrlMetaDataRetrieverImpl.UrlMetadataImpl(item.getUrl(), item.getResolvedUrl(), item.getMimeType(), item.getTitle(), item.getHasImage(), item.getHasVideo(), item.getDateResolved());
    }

    private <T> T getEntity(long id, Class<T> entityClass) {
        String sqlQuery = "select en from %s en where en.id = :id".formatted(entityClass.getSimpleName());
        TypedQuery<T> query = em.createQuery(sqlQuery, entityClass);
        return query
                .setParameter("id", id)
                .getSingleResult();
    }

    private Instant daysFromNow(int days) {
        LocalDateTime localDateTime = LocalDateTime.now().plusHours(days);
        return localDateTime.toInstant(ZoneOffset.UTC);
    }

    private AddItemRequest makeItemRequest(String url, Set<String> tags) {
        return AddItemRequest.builder()
                .url(url)
                .tags(tags)
                .build();
    }

    private ItemDto makeItemDto(
            User user,
            boolean unread,
            Set<String> tags,
            UrlMetaDataRetriever.UrlMetadata urlMetadata
    ) {
        return mapToDto(
                user.getId(),
                urlMetadata,
                unread,
                tags
        );
    }

    public ItemDto mapToDto(Long userId, UrlMetaDataRetriever.UrlMetadata urlMetadata, boolean unread, Set<String> tags) {
        if (userId == null && urlMetadata == null && tags == null) {
            return null;
        }

        ItemDto.ItemDtoBuilder itemDto = ItemDto.builder();

        if (urlMetadata != null) {
            itemDto.normalUrl(urlMetadata.getNormalUrl());
            itemDto.resolvedUrl(urlMetadata.getResolvedUrl());
            itemDto.mimeType(urlMetadata.getMimeType());
            itemDto.title(urlMetadata.getTitle());
            itemDto.hasImage(urlMetadata.isHasImage());
            itemDto.hasVideo(urlMetadata.isHasVideo());
            itemDto.dateResolved(instantToString(urlMetadata.getDateResolved()));
        }
        itemDto.unread(unread);
        if (tags != null) {
            itemDto.tags(new LinkedHashSet<>(tags));
        }

        return itemDto.build();
    }

    private Item makeItem(
            User user,
            String url,
            String resolvedUrl,
            String mimeType,
            String title,
            Boolean hasImage,
            Boolean hasVideo,
            Instant dateResolved,
            Boolean unread,
            Set<String> tags
    ) {
        Item item = new Item();

        item.setUser(user);
        item.setUrl(url);
        item.setResolvedUrl(resolvedUrl);
        item.setMimeType(mimeType);
        item.setTitle(title);
        item.setHasImage(hasImage);
        item.setHasVideo(hasVideo);
        item.setDateResolved(dateResolved);
        item.setUnread(unread);
        item.setTags(tags);

        return item;
    }

    private Item makeDefaultItem(
            User user
    ) {
        Item item = new Item();

        item.setUser(user);
        item.setUrl("https://bit.ly/3vRVvO0");
        item.setResolvedUrl("https://practicum.yandex.ru/java-developer/");
        item.setMimeType("text");
        item.setTitle("Курс «Java-разработчик» с нуля: онлайн-обучение Java-программированию для начинающих — Яндекс Практикум");
        item.setHasImage(true);
        item.setHasVideo(false);
        item.setDateResolved(daysFromNow(-365));
        item.setUnread(true);
        item.setTags(new HashSet<>(Set.of("yandex", "practicum")));

        return item;
    }

    private User makeDefaultUser(
    ) {
        User user = new User();
        user.setLastName("lastname");
        user.setFirstName("firstname");
        user.setRegistrationDate(Instant.now());
        user.setEmail("email");
        user.setState(UserState.ACTIVE);
        return user;
    }

    private Instant stringToInstant(String string) {
        LocalDateTime ldt = LocalDateTime.parse(string, FORMATTER);
        ZonedDateTime ztd = ldt.atZone(ZoneId.systemDefault());
        return ztd.toInstant();
    }

    private String instantToString(Instant instant) {
        return FORMATTER.format(instant);
    }
}
