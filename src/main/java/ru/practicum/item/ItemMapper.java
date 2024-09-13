package ru.practicum.item;

import ru.practicum.item.dto.AddItemRequest;
import ru.practicum.item.dto.ItemDto;
import ru.practicum.user.User;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

final class ItemMapper {

    public static Item mapToEntity(AddItemRequest request, User user) {
        Item item = new Item();
        item.setUser(user);
        item.setUrl(request.getUrl());
        item.setTags(request.getTags());

        return item;
    }

    public static Item mapToEntity(UrlMetaDataRetriever.UrlMetadata urlMetadata, User user, Set<String> tags) {
        Item item = new Item();
        item.setUrl(urlMetadata.getNormalUrl());
        item.setResolvedUrl(urlMetadata.getResolvedUrl());
        item.setMimeType(urlMetadata.getMimeType());
        item.setTitle(urlMetadata.getTitle());
        item.setHasImage(urlMetadata.isHasImage());
        item.setHasVideo(urlMetadata.isHasVideo());
        item.setDateResolved(urlMetadata.getDateResolved());
        item.setUser(user);
        item.setTags(tags);
        item.setUnread(Boolean.TRUE);
        return item;
    }

    public static ItemDto mapToDto(Item item) {
        return ItemDto.builder()
                .id(item.getId())
                .userId(item.getUser().getId())
                .url(item.getUrl())
                .build();
    }

    public static List<ItemDto> mapToDto(Iterable<Item> items) {
        List<ItemDto> itemDtos = new LinkedList<>();

        for (Item item : items) {
            ItemDto itemDto = mapToDto(item);
            itemDtos.add(itemDto);
        }
        return itemDtos;
    }
}
