package ru.practicum.item.dto;

import lombok.Data;

import java.util.Set;

@Data
public class ModifyItemRequest {
    private Long id;

    private Boolean unread;

    private Set<String> tags;

    private Boolean replaceTags;
}
