package ru.practicum.item;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.instancio.junit.InstancioExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.List;
import java.util.Set;

import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@Transactional
@ExtendWith(InstancioExtension.class)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@SpringJUnitConfig({AppConfig.class, PersistenceConfig.class, ItemServiceImpl.class, UrlMetaDataRetrieverImpl.class})
public class ItemServiceIntegrationTest {
    private final EntityManager em;
    private final ItemService itemService;

    @Test
    @Sql({"/db/sql/users.sql"})
    void addNewItem() {
        AddItemRequest request = makeItemRequest("https://bit.ly/3vRVvO0", Set.of());
        itemService.addNewItem(1L, request);

        TypedQuery<Item> query = em.createQuery("select it from Item as it where it.url = :url", Item.class);
        Item item = query.setParameter("url", request.getUrl())
                .getSingleResult();

        assertThat(item.getId(), notNullValue());
        assertThat(item.getUser().getId(), equalTo(1L));
        assertThat(item.getUser().getEmail(), equalTo("atabekbekseiit@gmail.com"));
        assertThat(item.getUser().getState(), equalTo(UserState.ACTIVE));
        assertThat(item.getUrl(), equalTo("https://bit.ly/3vRVvO0"));
        assertThat(item.getHasImage(), equalTo(true));
        assertThat(item.getUnread(), equalTo(true));
    }

    @Test
    @Sql({"/db/sql/users.sql"})
    void getItems_shouldReturnAllSavedItems_whenFilterAreNotStrong() {

        // given
        Long userId = 1L;
        User user = getEntity(userId, User.class);
        List<Item> items = List.of(
                makeItem(user, "https://bit.ly/3vRVvO0", "https://practicum.yandex.ru/java-developer/", "text",
                        "Курс «Java-разработчик» с нуля: онлайн-обучение Java-программированию для начинающих — Яндекс Практикум", true, true,
                        daysFromNow(730), true, Set.of("yandex", "practicum")),
                makeItem(user, "https://some-video-url", "https://some-resolved-url-video.com", "video",
                        "some title", false, true,
                        daysFromNow(365), false, Set.of("video")),
                makeItem(user, "https://some-image-url", "https://some-resolved-url-image.com", "image",
                        "some title", true, false,
                        daysFromNow(300), true, Set.of("image"))
        );

        for (Item item : items) {
            em.persist(item);
        }

        // when
        List<ItemDto> result = itemService.getItems(new GetItemRequest(
                1L,
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
    @Sql({"/db/sql/users.sql", "/db/sql/items.sql"})
    void edit_shouldReturnUpdatedItem_whenEverythingIsOK() {

        // given
        Long userId = 1L;
        User user = getEntity(userId, User.class);

        Long itemId = 1L;
        Item item = getEntity(itemId, Item.class);

        // when
        ModifyItemRequest editRequest = new ModifyItemRequest();
        editRequest.setUnread(false);
        editRequest.setReplaceTags(true);
        editRequest.setTags(Set.of("shuk", "laki"));
        itemService.edit(userId, editRequest);
    }


    private <T> T getEntity(Long id, Class<T> entityClass) {
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
}
