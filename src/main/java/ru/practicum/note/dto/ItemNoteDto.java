package ru.practicum.note.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class ItemNoteDto {

    private Long id;
    private Long itemId;
    private String text;
    private String dateOfNote;
    private String itemUrl;
}

