package com.tencent.supersonic.semantic.api.query.pojo;

import com.tencent.supersonic.semantic.api.query.enums.FilterOperatorEnum;

import java.util.Arrays;
import java.util.List;

import lombok.Data;

@Data
public class Criterion {

    private String column;

    private FilterOperatorEnum operator;

    private Object value;

    private List<Object> values;

    private String dataType;

    public Criterion(String column, FilterOperatorEnum operator, Object value, String dataType) {
        super();
        this.column = column;
        this.operator = operator;
        this.value = value;
        this.dataType = dataType;

        if (FilterOperatorEnum.BETWEEN.name().equals(operator) || FilterOperatorEnum.IN.name().equals(operator)
                || FilterOperatorEnum.NOT_IN.name().equals(operator)) {
            this.values = (List) value;
        }
    }

    public boolean isNeedApostrophe() {
        return Arrays.stream(StringDataType.values())
                .filter(value -> this.dataType.equalsIgnoreCase(value.getType())).findFirst()
                .isPresent();
    }


    public enum NumericDataType {
        TINYINT("TINYINT"),
        SMALLINT("SMALLINT"),
        MEDIUMINT("MEDIUMINT"),
        INT("INT"),
        INTEGER("INTEGER"),
        BIGINT("BIGINT"),
        FLOAT("FLOAT"),
        DOUBLE("DOUBLE"),
        DECIMAL("DECIMAL"),
        NUMERIC("NUMERIC"),
        ;
        private String type;

        NumericDataType(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }
    }


    public enum StringDataType {
        VARCHAR("VARCHAR"),
        STRING("STRING"),
        ;
        private String type;

        StringDataType(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }
    }


}
