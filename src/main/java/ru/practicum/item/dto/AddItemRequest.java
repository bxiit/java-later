package ru.practicum.item.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Jacksonized
public class AddItemRequest {
    private String url;
    private Set<String> tags;
}
