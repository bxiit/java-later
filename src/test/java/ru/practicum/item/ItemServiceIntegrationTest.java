package ru.practicum.item;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;
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
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Transactional
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@SpringJUnitConfig({AppConfig.class, PersistenceConfig.class, ItemServiceImpl.class, UrlMetaDataRetrieverImpl.class})
@TestPropertySource(properties = {
        "jdbc.url=jdbc:postgresql://localhost:5432/test",
        "hibernate.hbm2ddl.auto=update"
})
public class ItemServiceIntegrationTest {
    private final EntityManager em;
    private final ItemService itemService;

    @Test
    void addNewItem() {
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
    void getItems_shouldReturnAllSavedItems_whenFilterAreNotStrong() {

        // given
        User defaultUser = makeDefaultUser();
        em.persist(defaultUser);

        List<Item> items = List.of(
                makeItem(defaultUser, "https://bit.ly/3vRVvO0", "https://practicum.yandex.ru/java-developer/", "text",
                        "Курс «Java-разработчик» с нуля: онлайн-обучение Java-программированию для начинающих — Яндекс Практикум", true, true,
                        daysFromNow(730), true, Set.of("yandex", "practicum")),
                makeItem(defaultUser, "https://some-video-url", "https://some-resolved-url-video.com", "video",
                        "some title", false, true,
                        daysFromNow(365), false, Set.of("video")),
                makeItem(defaultUser, "https://some-image-url", "https://some-resolved-url-image.com", "image",
                        "some title", true, false,
                        daysFromNow(300), true, Set.of("image"))
        );

        for (Item item : items) {
            em.persist(item);
        }

        // when
        List<ItemDto> result = itemService.getItems(new GetItemRequest(
                defaultUser.getId(),
                GetItemRequest.State.ALL,
                GetItemRequest.ContentType.ALL,
                GetItemRequest.Sort.NEWEST,
                5,
                emptyList()
        ));

        // then
        assertThat(result.size(), equalTo(3));
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
            Long userId,
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
        return ItemDto.builder()
                .userId(userId)
                .url(url)
                .resolvedUrl(resolvedUrl)
                .mimeType(mimeType)
                .title(title)
                .hasImage(hasImage)
                .hasVideo(hasVideo)
                .dateResolved(dateResolved)
                .unread(unread)
                .tags(tags)
                .build();
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
        return Item.builder()
                .user(user)
                .url(url)
                .resolvedUrl(resolvedUrl)
                .mimeType(mimeType)
                .title(title)
                .hasImage(hasImage)
                .hasVideo(hasVideo)
                .dateResolved(dateResolved)
                .unread(unread)
                .tags(tags)
                .build();
    }

    private Item makeDefaultItem(
            User user
    ) {
        return Item.builder()
                .user(user)
                .url("https://bit.ly/3vRVvO0")
                .resolvedUrl("https://practicum.yandex.ru/java-developer/")
                .mimeType("text")
                .title("Курс «Java-разработчик» с нуля: онлайн-обучение Java-программированию для начинающих — Яндекс Практикум")
                .hasImage(true)
                .hasVideo(false)
                .dateResolved(daysFromNow(-365))
                .unread(true)
                .tags(new HashSet<>(Set.of("yandex", "practicum")))
                .build();
    }

    private User makeUser(
            String lName,
            String fName,
            Instant regDate,
            String email,
            UserState state
    ) {
        User user = new User();
        user.setLastName(lName);
        user.setFirstName(fName);
        user.setRegistrationDate(regDate);
        user.setEmail(email);
        user.setState(state);
        return user;
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
}
