package com.tencent.supersonic.chat.application.query;

import com.tencent.supersonic.chat.domain.pojo.chat.SchemaElementOption;
import lombok.Data;

@Data
public class QueryMatchOption {

    private SchemaElementOption schemaElementOption;
    private RequireNumberType requireNumberType;
    private Integer requireNumber;

    public static QueryMatchOption build(SchemaElementOption schemaElementOption,
            RequireNumberType requireNumberType, Integer requireNumber) {
        QueryMatchOption queryMatchOption = new QueryMatchOption();
        queryMatchOption.requireNumber = requireNumber;
        queryMatchOption.requireNumberType = requireNumberType;
        queryMatchOption.schemaElementOption = schemaElementOption;
        return queryMatchOption;
    }

    public static QueryMatchOption optional() {
        QueryMatchOption queryMatchOption = new QueryMatchOption();
        queryMatchOption.setSchemaElementOption(SchemaElementOption.OPTIONAL);
        queryMatchOption.setRequireNumber(0);
        queryMatchOption.setRequireNumberType(RequireNumberType.AT_LEAST);
        return queryMatchOption;
    }

    public static QueryMatchOption unused() {
        QueryMatchOption queryMatchOption = new QueryMatchOption();
        queryMatchOption.setSchemaElementOption(SchemaElementOption.UNUSED);
        queryMatchOption.setRequireNumber(0);
        queryMatchOption.setRequireNumberType(RequireNumberType.EQUAL);
        return queryMatchOption;
    }

    public enum RequireNumberType {
        AT_MOST, AT_LEAST, EQUAL
    }


}
