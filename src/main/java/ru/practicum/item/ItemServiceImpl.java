package ru.practicum.item;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.exception.AccessException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.item.dto.AddItemRequest;
import ru.practicum.item.dto.GetItemRequest;
import ru.practicum.item.dto.ItemDto;
import ru.practicum.item.dto.ModifyItemRequest;
import ru.practicum.user.User;
import ru.practicum.user.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final UrlMetaDataRetriever urlMetaDataRetriever;

    @Override
    @Transactional
    public ItemDto addNewItem(Long userId, AddItemRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        UrlMetaDataRetriever.UrlMetadata urlMetadata = urlMetaDataRetriever.retrieve(request.getUrl());

        final Item item;
        Optional<Item> maybeExistingItem = itemRepository.findByUserAndResolvedUrl(user, urlMetadata.getResolvedUrl());

        if (maybeExistingItem.isEmpty()) {
            item = itemRepository.save(ItemMapper.mapToEntity(urlMetadata, user, request.getTags()));
        } else {
            item = maybeExistingItem.get();
            if (request.getTags() != null && !request.getTags().isEmpty()) {
                item.getTags().addAll(request.getTags());
                itemRepository.save(item);
            }
        }

        return ItemMapper.mapToDto(item);
    }

    @Override
    public void deleteItem(long userId, long itemId) {
        itemRepository.deleteItemByUserIdAndId(userId, itemId);
    }

    @Override
    public List<ItemDto> getItems(GetItemRequest request) {
        Sort sort = request.getSort().get();
        PageRequest pageRequest = PageRequest.of(0, request.getLimit(), sort);

        List<BooleanExpression> conditions = new ArrayList<>();
        conditions.add(makeStateExpression(request));
        conditions.add(makeContentTypeExpression(request));
        conditions.add(makeOwnerExpression(request));
        conditions.add(makeTagsExpression(request));

        Iterable<Item> items = itemRepository.findAll(makeSingleExpression(conditions), pageRequest);
        return StreamSupport.stream(items.spliterator(), false)
                .map(ItemMapper::mapToDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemDto> getItems(long userId, Set<String> tags) {
        BooleanExpression byUserId = QItem.item.user.id.eq(userId);
        BooleanExpression byAnyTag = QItem.item.tags.any().in(tags);
        Iterable<Item> items = itemRepository.findAll(byUserId.and(byAnyTag));
        return ItemMapper.mapToDto(items);
    }

    @Override
    @Transactional
    public void edit(long userId, ModifyItemRequest request) {
        Item item = itemRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException("errors.404.items"));
        if (!item.getUser().getId().equals(userId)) {
            throw new AccessException(HttpStatus.FORBIDDEN, "errors.403.items");
        }

        if (request.getUnread() != null) {
            item.setUnread(request.getUnread());
        }

        if (request.getReplaceTags()) {
            item.setTags(request.getTags());
        } else {
            item.getTags().addAll(request.getTags());
        }
    }

    private BooleanExpression makeSingleExpression(List<BooleanExpression> conditions) {
        return Expressions.allOf(conditions.toArray(new BooleanExpression[0]));
    }

    private BooleanExpression makeTagsExpression(GetItemRequest request) {
        return Optional.ofNullable(request.getTags())
                .filter(tags -> !tags.isEmpty())
                .map(tags -> QItem.item.tags.any().in(tags))
                .orElse(null);
    }

    private BooleanExpression makeOwnerExpression(GetItemRequest request) {
        return QItem.item.user.id.eq(request.getUserId());
    }

    private BooleanExpression makeStateExpression(GetItemRequest request) {
        return request.getState().get();
    }

    private BooleanExpression makeContentTypeExpression(GetItemRequest request) {
        return request.getContentType().get();
    }
}
