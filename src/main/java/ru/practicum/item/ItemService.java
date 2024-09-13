package ru.practicum.item;

import ru.practicum.item.dto.AddItemRequest;
import ru.practicum.item.dto.GetItemRequest;
import ru.practicum.item.dto.ItemDto;
import ru.practicum.item.dto.ModifyItemRequest;

import java.util.List;
import java.util.Set;

public interface ItemService {

    ItemDto addNewItem(Long userId, AddItemRequest request);

    void deleteItem(long userId, long itemId);

    List<ItemDto> getItems(GetItemRequest userId);

    List<ItemDto> getItems(long userId, Set<String> tags);

    void edit(long userId, ModifyItemRequest request);
}
