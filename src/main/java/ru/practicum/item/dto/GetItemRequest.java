package ru.practicum.item.dto;

import com.querydsl.core.types.dsl.BooleanExpression;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.practicum.item.QItem;
import ru.practicum.item.dto.condition.ContentTypeCondition;
import ru.practicum.item.dto.condition.SortCondition;
import ru.practicum.item.dto.condition.StateCondition;

import java.util.List;

@Getter
@AllArgsConstructor
public class GetItemRequest {
    private final long userId;
    private final State state;
    private final ContentType contentType;
    private final Sort sort;
    private final int limit;
    private final List<String> tags;

    @Getter
    @RequiredArgsConstructor
    public enum ContentType implements ContentTypeCondition {
        ALL(null),
        ARTICLE(QItem.item.mimeType.startsWith("text")),
        IMAGE(QItem.item.mimeType.startsWith("img")),
        VIDEO(QItem.item.mimeType.startsWith("video"));
        private final BooleanExpression expr;

        @Override
        public BooleanExpression get() {
            return expr;
        }
    }

    @Getter
    @RequiredArgsConstructor
    public enum State implements StateCondition {
        ALL(null),
        UNREAD(QItem.item.unread.isTrue()),
        READ(QItem.item.unread.isFalse());
        private final BooleanExpression expr;

        @Override
        public BooleanExpression get() {
            return expr;
        }
    }

    @Getter
    @RequiredArgsConstructor
    public enum Sort implements SortCondition {

        NEWEST(org.springframework.data.domain.Sort.by("dateResolved").descending()),
        OLDEST(org.springframework.data.domain.Sort.by("dateResolved").ascending()),
        TITLE(org.springframework.data.domain.Sort.by("title"));
        private final org.springframework.data.domain.Sort sort;

        @Override
        public org.springframework.data.domain.Sort get() {
            return getSort();
        }
    }
}
