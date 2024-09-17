package ru.practicum.item.dto;

import lombok.Builder;
import lombok.Getter;
import ru.practicum.item.UrlMetaDataRetriever;
import ru.practicum.item.UrlMetaDataRetrieverImpl;

import java.util.Set;

@Getter
@Builder(toBuilder = true)
public class ItemDto {
    private final Long id;
    private final String normalUrl;
    private final String resolvedUrl;
    private final String mimeType;
    private final String title;
    private final Boolean hasImage;
    private final Boolean hasVideo;
    private final Boolean unread;
    private final String dateResolved;
    private final Set<String> tags;
}