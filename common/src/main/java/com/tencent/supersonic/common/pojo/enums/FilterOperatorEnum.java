package com.tencent.supersonic.common.pojo.enums;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

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
            if (operatorEnum.value.equalsIgnoreCase(type) || operatorEnum.name().equalsIgnoreCase(type)) {
                return operatorEnum;
            }
        }
        return null;
    }

    @JsonValue
    public String getValue() {
        return value;
    }


}
