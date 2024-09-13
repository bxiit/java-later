package ru.practicum.item.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Set;

@Data
@Builder
public class ItemDto {
    private Long id;

    private Long userId;

    private String url;

    private String resolvedUrl;

    private String mimeType;

    private String title;

    private Boolean hasImage;

    private Boolean hasVideo;

    private Instant dateResolved;

    private Boolean unread;

    private Set<String> tags;
}
