package ru.practicum.item.dto;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
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

    public enum ContentType implements ContentTypeCondition {
        ALL {
            @Override
            public BooleanExpression get() {
                // null for purpose
                return null;
            }
        },
        ARTICLE {
            @Override
            public BooleanExpression get() {
                return QItem.item.mimeType.equalsIgnoreCase("text");
            }
        },
        IMAGE {
            @Override
            public BooleanExpression get() {
                return QItem.item.mimeType.equalsIgnoreCase("img");
            }
        },
        VIDEO {
            @Override
            public BooleanExpression get() {
                return QItem.item.mimeType.equalsIgnoreCase("video");
            }
        }
    }
    public enum State implements StateCondition {
        ALL {
            @Override
            public BooleanExpression get() {
                // null for purpose
                return null;
            }
        },
        UNREAD {
            @Override
            public BooleanExpression get() {
                return QItem.item.unread.isTrue();
            }
        },
        READ {
            @Override
            public BooleanExpression get() {
                return QItem.item.unread.isFalse();
            }
        };
    }

    @Getter
    @RequiredArgsConstructor
    public enum Sort implements SortCondition {

        NEWEST (org.springframework.data.domain.Sort.by("dateResolved").descending()),
        OLDEST (org.springframework.data.domain.Sort.by("dateResolved").ascending()),
        TITLE (org.springframework.data.domain.Sort.by("title"));
        private final org.springframework.data.domain.Sort sort;

        @Override
        public org.springframework.data.domain.Sort get() {
            return getSort();
        }
    }
}
