package com.tencent.supersonic.chat.application.query;

import com.tencent.supersonic.chat.domain.pojo.chat.SchemaElementOption;
import lombok.Data;

@Data
public class QueryModeElementOption {

    public enum RequireNumberType {
        AT_MOST, AT_LEAST, EQUAL
    }

    private SchemaElementOption schemaElementOption;
    private RequireNumberType requireNumberType;
    private Integer requireNumber;

    public static QueryModeElementOption build(SchemaElementOption schemaElementOption,
            RequireNumberType requireNumberType, Integer requireNumber) {
        QueryModeElementOption queryModeElementOption = new QueryModeElementOption();
        queryModeElementOption.requireNumber = requireNumber;
        queryModeElementOption.requireNumberType = requireNumberType;
        queryModeElementOption.schemaElementOption = schemaElementOption;
        return queryModeElementOption;
    }

    public static QueryModeElementOption optional() {
        QueryModeElementOption queryModeElementOption = new QueryModeElementOption();
        queryModeElementOption.setSchemaElementOption(SchemaElementOption.OPTIONAL);
        queryModeElementOption.setRequireNumber(0);
        queryModeElementOption.setRequireNumberType(RequireNumberType.AT_LEAST);
        return queryModeElementOption;
    }

    public static QueryModeElementOption unused() {
        QueryModeElementOption queryModeElementOption = new QueryModeElementOption();
        queryModeElementOption.setSchemaElementOption(SchemaElementOption.UNUSED);
        queryModeElementOption.setRequireNumber(0);
        queryModeElementOption.setRequireNumberType(RequireNumberType.EQUAL);
        return queryModeElementOption;
    }


}
