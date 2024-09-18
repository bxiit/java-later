package ru.practicum.note;

import lombok.experimental.UtilityClass;
import ru.practicum.item.Item;
import ru.practicum.note.dto.ItemNoteDto;

@UtilityClass
public class ItemNoteMapper {
    public static ItemNoteDto mapToDto(ItemNote itemNote) {
        return ItemNoteDto.of(
                itemNote.getId(), itemNote.getItem().getId(), itemNote.getText(), null, itemNote.getItem().getUrl()
        );
    }

    public static ItemNote mapToEntity(ItemNoteDto itemNoteDto, Item item) {
        return ItemNote.of(
                itemNoteDto.getItemId(), itemNoteDto.getText(), item
        );
    }
}
