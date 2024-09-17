package ru.practicum.item;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.common.NotFoundException;
import ru.practicum.item.dto.AddItemRequest;
import ru.practicum.item.dto.ItemDto;
import ru.practicum.user.User;
import ru.practicum.user.UserRepository;
import ru.practicum.user.UserState;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemServiceImplTest {

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
                    User user = makeUser(userId, "atabekbekseiit@gmail.com", "Bexeiit", "Atabek",
                            LocalDateTime.of(2010, Month.JANUARY, 1, 12, 0), UserState.ACTIVE);

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

        AddItemRequest request = AddItemRequest.builder()
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
            AddItemRequest request = AddItemRequest.builder().build();
            itemService.addNewItem(999L, request);
        });
        verify(userRepository).findById(anyLong());
        verifyNoInteractions(itemRepository);
    }

    private User makeUser(Long userId, String email, String fName, String lName, LocalDateTime regDateTime, UserState userState) {
        User user = new User();
        user.setId(userId);
        user.setEmail(email);
        user.setFirstName(fName);
        user.setLastName(lName);
        user.setRegistrationDate(regDateTime.toInstant(ZoneOffset.UTC));
        user.setState(userState);
        return user;
    }

    private UrlMetaDataRetriever.UrlMetadata makeUrlMetaData(
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

    private Instant hoursFromNow(int hours) {
        LocalDateTime localDateTime = LocalDateTime.now().plusHours(hours);
        return localDateTime.toInstant(ZoneOffset.UTC);
    }
}