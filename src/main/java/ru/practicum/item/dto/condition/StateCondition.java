package ru.practicum.item.dto.condition;

import com.querydsl.core.types.dsl.BooleanExpression;

public interface StateCondition {
    BooleanExpression get();
}
