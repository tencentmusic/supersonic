package com.tencent.supersonic.headless.chat.utils;

import com.tencent.supersonic.common.pojo.enums.FilterOperatorEnum;
import com.tencent.supersonic.headless.api.pojo.request.QueryFilter;
import com.tencent.supersonic.headless.api.pojo.request.QueryFilters;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class QueryFilterParser {

    public static String parse(QueryFilters queryFilters) {
        try {
            List<String> conditions = queryFilters.getFilters().stream()
                    .map(QueryFilterParser::parseFilter).collect(Collectors.toList());
            return String.join(" AND ", conditions);
        } catch (Exception e) {
            log.error("", e);
        }
        return null;
    }

    private static String parseFilter(QueryFilter filter) {
        String column = filter.getName();
        FilterOperatorEnum operator = filter.getOperator();
        Object value = filter.getValue();

        switch (operator) {
            case IN:
            case NOT_IN:
                return column + " " + operator.getValue() + " (" + parseList(value) + ")";
            case BETWEEN:
                if (value instanceof List && ((List<?>) value).size() == 2) {
                    List<?> values = (List<?>) value;
                    return column + " BETWEEN " + formatValue(values.get(0)) + " AND "
                            + formatValue(values.get(1));
                }
                throw new IllegalArgumentException(
                        "BETWEEN operator requires a list of two values");
            case IS_NULL:
            case IS_NOT_NULL:
                return column + " " + operator.getValue();
            case EXISTS:
                return "EXISTS (" + value + ")";
            case SQL_PART:
                return value.toString();
            default:
                return column + " " + operator.getValue() + " " + formatValue(value);
        }
    }

    private static String parseList(Object value) {
        if (value instanceof List) {
            return ((List<?>) value).stream().map(QueryFilterParser::formatValue)
                    .collect(Collectors.joining(", "));
        }
        throw new IllegalArgumentException("IN and NOT IN operators require a list of values");
    }

    private static String formatValue(Object value) {
        if (value instanceof String) {
            return "'" + value + "'";
        } else if (value instanceof Number) {
            return value.toString();
        } else if (value instanceof Boolean) {
            return (Boolean) value ? "TRUE" : "FALSE";
        }
        throw new IllegalArgumentException("Unsupported value type: " + value.getClass());
    }
}
