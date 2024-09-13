package ru.practicum.item;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.item.dto.AddItemRequest;
import ru.practicum.item.dto.GetItemRequest;
import ru.practicum.item.dto.ItemDto;
import ru.practicum.item.dto.ModifyItemRequest;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
public class ItemController {
    private final ItemService itemService;

    @GetMapping()
    public List<ItemDto> get(
            @RequestHeader("X-Later-User-Id") long userId,
            @RequestParam(name = "state", defaultValue = "unread") String state,
            @RequestParam(name = "contentType", defaultValue = "all") String contentType,
            @RequestParam(name = "sort", defaultValue = "newest") String sort,
            @RequestParam(name = "limit", defaultValue = "10") int limit,
            @RequestParam(name = "tags", required = false) List<String> tags
    ) {
        return itemService.getItems(new GetItemRequest(
                userId,
                GetItemRequest.State.valueOf(state),
                GetItemRequest.ContentType.valueOf(contentType),
                GetItemRequest.Sort.valueOf(sort),
                limit,
                tags));
    }

    @PostMapping
    public ItemDto add(@RequestHeader("X-Later-User-Id") long userId,
                       @RequestBody AddItemRequest request) {
        return itemService.addNewItem(userId, request);
    }

    @DeleteMapping("/{itemId}")
    public void deleteItem(@RequestHeader("X-Later-User-Id") long userId,
                           @PathVariable(name = "itemId") long itemId) {
        itemService.deleteItem(userId, itemId);
    }

    @GetMapping("/by-tags")
    public List<ItemDto> getFilteredItems(
            @RequestHeader("X-Later-User-Id") long userId,
            @RequestParam("tag") String[] tag
    ) {
        return itemService.getItems(userId, Set.of(tag));
    }

    @PatchMapping
    public void editItem(
            @RequestHeader("X-Later-User-Id") long userId,
            @RequestBody ModifyItemRequest request
            ) {
        itemService.edit(userId, request);
    }
}