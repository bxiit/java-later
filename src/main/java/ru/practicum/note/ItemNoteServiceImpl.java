package ru.practicum.note;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import ru.practicum.exception.NotFoundException;
import ru.practicum.item.Item;
import ru.practicum.item.ItemRepository;
import ru.practicum.note.dto.ItemNoteDto;
import ru.practicum.note.mapper.ItemNoteMapper;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ItemNoteServiceImpl implements ItemNoteService {

    private final ItemNoteRepository itemNoteRepository;
    private final ItemRepository itemRepository;

    @Override
    public ItemNoteDto addNewItemNote(long userId, ItemNoteDto itemNoteDto) {
        Item item = itemRepository.findById(itemNoteDto.getItemId())
                .orElseThrow(() -> new NotFoundException("Item not found"));
        ItemNote itemNote = ItemNoteMapper.mapToEntity(itemNoteDto, item);
        itemNote = itemNoteRepository.save(itemNote);
        return ItemNoteMapper.mapToDto(itemNote);
    }

    @Override
    public List<ItemNoteDto> searchNotesByUrl(String url, Long userId) {
        return itemNoteRepository.findAllByItemUrlContainingAndItemUserId(url, userId).stream()
                .map(ItemNoteMapper::mapToDto)
                .toList();
    }

    @Override
    public List<ItemNoteDto> searchNotesByTag(long userId, String tag) {
        return itemNoteRepository.findItemNotesByUsersTags(userId, tag).stream()
                .map(ItemNoteMapper::mapToDto)
                .toList();
    }

    @Override
    public List<ItemNoteDto> listAllItemsWithNotes(long userId, int from, int size) {
        PageRequest pageRequest = PageRequest.of(from > 0 ? from / size : 0, size);
        return itemNoteRepository.findAllByItemUserId(userId, pageRequest).stream()
                .map(ItemNoteMapper::mapToDto)
                .toList();
    }
}
