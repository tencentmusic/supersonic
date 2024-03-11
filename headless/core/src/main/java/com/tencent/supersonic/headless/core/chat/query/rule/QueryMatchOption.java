package com.tencent.supersonic.headless.core.chat.query.rule;

import lombok.Data;

@Data
public class QueryMatchOption {

    private OptionType schemaElementOption;
    private RequireNumberType requireNumberType;
    private Integer requireNumber;

    public static QueryMatchOption build(OptionType schemaElementOption,
            RequireNumberType requireNumberType, Integer requireNumber) {
        QueryMatchOption queryMatchOption = new QueryMatchOption();
        queryMatchOption.requireNumber = requireNumber;
        queryMatchOption.requireNumberType = requireNumberType;
        queryMatchOption.schemaElementOption = schemaElementOption;
        return queryMatchOption;
    }

    public static QueryMatchOption optional() {
        QueryMatchOption queryMatchOption = new QueryMatchOption();
        queryMatchOption.setSchemaElementOption(OptionType.OPTIONAL);
        queryMatchOption.setRequireNumber(0);
        queryMatchOption.setRequireNumberType(RequireNumberType.AT_LEAST);
        return queryMatchOption;
    }

    public static QueryMatchOption unused() {
        QueryMatchOption queryMatchOption = new QueryMatchOption();
        queryMatchOption.setSchemaElementOption(OptionType.UNUSED);
        queryMatchOption.setRequireNumber(0);
        queryMatchOption.setRequireNumberType(RequireNumberType.EQUAL);
        return queryMatchOption;
    }

    public enum RequireNumberType {
        AT_MOST, AT_LEAST, EQUAL
    }

    public enum OptionType {
        REQUIRED, OPTIONAL, UNUSED
    }

}
