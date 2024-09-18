package ru.practicum.item;

import ru.practicum.item.dto.AddItemRequest;
import ru.practicum.item.dto.ItemDto;
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
import java.util.Set;

public class ItemServiceTest {

    protected static final DateTimeFormatter FORMATTER = DateTimeFormatter
            .ofPattern("yyyy.MM.dd HH:mm:ss")
            .withZone(ZoneOffset.UTC);

    protected UrlMetaDataRetrieverImpl.UrlMetadataImpl extractUrlMetaDataFromItemDto(ItemDto itemDto) {
        return new UrlMetaDataRetrieverImpl.UrlMetadataImpl(itemDto.getNormalUrl(), itemDto.getResolvedUrl(), itemDto.getMimeType(), itemDto.getTitle(), itemDto.getHasImage(), itemDto.getHasVideo(), stringToInstant(itemDto.getDateResolved()));
    }

    protected UrlMetaDataRetrieverImpl.UrlMetadataImpl extractUrlMetaDataFromItem(Item item) {
        return new UrlMetaDataRetrieverImpl.UrlMetadataImpl(item.getUrl(), item.getResolvedUrl(), item.getMimeType(), item.getTitle(), item.getHasImage(), item.getHasVideo(), item.getDateResolved());
    }

    protected User makeUser(Long userId, String email, String fName, String lName, LocalDateTime regDateTime, UserState userState) {
        User user = new User();
        user.setId(userId);
        user.setEmail(email);
        user.setFirstName(fName);
        user.setLastName(lName);
        user.setRegistrationDate(regDateTime.toInstant(ZoneOffset.UTC));
        user.setState(userState);
        return user;
    }

    protected UrlMetaDataRetriever.UrlMetadata makeUrlMetaData(
            String normalUrl,
            String resolvedUrl,
            String mimeType,
            String title,
            boolean hasImage,
            boolean hasVideo,
            Instant dateResolved
    ) {
        return new UrlMetaDataRetrieverImpl.UrlMetadataImpl(
                normalUrl,
                resolvedUrl,
                mimeType,
                title,
                hasImage,
                hasVideo,
                dateResolved
        );
    }

    protected Instant hoursFromNow(int hours) {
        LocalDateTime localDateTime = LocalDateTime.now().plusHours(hours);
        return localDateTime.toInstant(ZoneOffset.UTC);
    }

    protected Instant daysFromNow(int days) {
        LocalDateTime localDateTime = LocalDateTime.now().plusHours(days);
        return localDateTime.toInstant(ZoneOffset.UTC);
    }

    protected AddItemRequest makeItemRequest(String url, Set<String> tags) {
        return AddItemRequest.builder()
                .url(url)
                .tags(tags)
                .build();
    }

    protected ItemDto makeItemDto(
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

    protected Item makeItem(
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

    protected Item makeDefaultItem(User user) {
        return makeDefaultItemWithId(user, null);
    }

    protected Item makeDefaultItemWithId(User user, Long itemId) {
        Item item = new Item();

        item.setId(itemId);
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

    protected User makeDefaultUser() {
        return makeDefaultUserWithId(null);
    }

    protected User makeDefaultUserWithId(Long userId) {
        User user = new User();
        user.setId(userId);
        user.setLastName("lastname");
        user.setFirstName("firstname");
        user.setRegistrationDate(Instant.now());
        user.setEmail("email");
        user.setState(UserState.ACTIVE);
        return user;
    }

    protected Instant stringToInstant(String string) {
        LocalDateTime ldt = LocalDateTime.parse(string, FORMATTER);
        ZonedDateTime ztd = ldt.atZone(ZoneId.systemDefault());
        return ztd.toInstant();
    }

    protected String instantToString(Instant instant) {
        return FORMATTER.format(instant);
    }

}
