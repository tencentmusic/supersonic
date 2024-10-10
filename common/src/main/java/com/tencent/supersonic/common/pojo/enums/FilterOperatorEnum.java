package com.tencent.supersonic.common.pojo.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import net.sf.jsqlparser.expression.operators.relational.ComparisonOperator;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals;
import net.sf.jsqlparser.expression.operators.relational.MinorThan;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;

public enum FilterOperatorEnum {
    IN("IN"),
    NOT_IN("NOT_IN"),
    EQUALS("="),
    BETWEEN("BETWEEN"),
    GREATER_THAN(">"),
    GREATER_THAN_EQUALS(">="),
    IS_NULL("IS_NULL"),
    IS_NOT_NULL("IS_NOT_NULL"),
    LIKE("LIKE"),
    MINOR_THAN("<"),
    MINOR_THAN_EQUALS("<="),
    NOT_EQUALS("!="),
    SQL_PART("SQL_PART"),
    EXISTS("EXISTS");

    private String value;

    FilterOperatorEnum(String value) {
        this.value = value;
    }

    @JsonCreator
    public static FilterOperatorEnum getSqlOperator(String type) {
        for (FilterOperatorEnum operatorEnum : FilterOperatorEnum.values()) {
            if (operatorEnum.value.equalsIgnoreCase(type)
                    || operatorEnum.name().equalsIgnoreCase(type)) {
                return operatorEnum;
            }
        }
        return null;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public static boolean isValueCompare(FilterOperatorEnum filterOperatorEnum) {
        return EQUALS.equals(filterOperatorEnum) || GREATER_THAN.equals(filterOperatorEnum)
                || GREATER_THAN_EQUALS.equals(filterOperatorEnum)
                || MINOR_THAN.equals(filterOperatorEnum)
                || MINOR_THAN_EQUALS.equals(filterOperatorEnum)
                || NOT_EQUALS.equals(filterOperatorEnum);
    }

    public static ComparisonOperator createExpression(FilterOperatorEnum operator) {
        switch (operator) {
            case EQUALS:
                return new EqualsTo();
            case GREATER_THAN_EQUALS:
                return new GreaterThanEquals();
            case GREATER_THAN:
                return new GreaterThan();
            case MINOR_THAN_EQUALS:
                return new MinorThanEquals();
            case MINOR_THAN:
                return new MinorThan();
            default:
                return null;
        }
    }
}
