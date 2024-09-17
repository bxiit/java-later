package ru.practicum.item.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class ModifyItemRequest {
    private Long id;

    private Boolean unread;

    private Set<String> tags;

    private Boolean replaceTags;

    public boolean hasTags() {
        return tags != null && !tags.isEmpty();
    }
}
