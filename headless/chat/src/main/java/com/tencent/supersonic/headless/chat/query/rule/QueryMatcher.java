package com.tencent.supersonic.headless.chat.query.rule;

import com.tencent.supersonic.common.pojo.enums.AggregateTypeEnum;
import com.tencent.supersonic.headless.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.headless.api.pojo.SchemaElementType;
import lombok.Data;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Data
@ToString
public class QueryMatcher {

    private HashMap<SchemaElementType, QueryMatchOption> elementOptionMap = new HashMap<>();
    private boolean supportCompare;
    private boolean supportOrderBy;
    private List<AggregateTypeEnum> orderByTypes =
            Arrays.asList(AggregateTypeEnum.MAX, AggregateTypeEnum.MIN, AggregateTypeEnum.TOPN);

    public QueryMatcher() {
        for (SchemaElementType type : SchemaElementType.values()) {
            if (type.equals(SchemaElementType.DATASET)) {
                elementOptionMap.put(type, QueryMatchOption.optional());
            } else {
                elementOptionMap.put(type, QueryMatchOption.unused());
            }
        }
    }

    public QueryMatcher addOption(SchemaElementType type, QueryMatchOption.OptionType option,
            QueryMatchOption.RequireNumberType requireNumberType, Integer requireNumber) {
        elementOptionMap.put(type,
                QueryMatchOption.build(option, requireNumberType, requireNumber));
        return this;
    }

    /**
     * Match schema element with current query according to the options.
     *
     * @param candidateElementMatches
     * @return a list of all matched schema elements, empty list if no matches can be found
     */
    public List<SchemaElementMatch> match(List<SchemaElementMatch> candidateElementMatches) {
        List<SchemaElementMatch> elementMatches = new ArrayList<>();
        HashMap<SchemaElementType, Integer> schemaElementTypeCount = new HashMap<>();
        for (SchemaElementMatch schemaElementMatch : candidateElementMatches) {
            SchemaElementType schemaElementType = schemaElementMatch.getElement().getType();
            if (schemaElementTypeCount.containsKey(schemaElementType)) {
                schemaElementTypeCount.put(schemaElementType,
                        schemaElementTypeCount.get(schemaElementType) + 1);
            } else {
                schemaElementTypeCount.put(schemaElementType, 1);
            }
        }

        // check if current query options are satisfied, return immediately if not
        for (Map.Entry<SchemaElementType, QueryMatchOption> e : elementOptionMap.entrySet()) {
            SchemaElementType elementType = e.getKey();
            QueryMatchOption elementOption = e.getValue();
            if (!isMatch(elementOption, getCount(schemaElementTypeCount, elementType))) {
                return new ArrayList<>();
            }
        }

        // add element match if its element type is not declared as unused
        for (SchemaElementMatch elementMatch : candidateElementMatches) {
            QueryMatchOption elementOption =
                    elementOptionMap.get(elementMatch.getElement().getType());
            if (Objects.nonNull(elementOption) && !elementOption.getSchemaElementOption()
                    .equals(QueryMatchOption.OptionType.UNUSED)) {
                elementMatches.add(elementMatch);
            }
        }

        return elementMatches;
    }

    private int getCount(HashMap<SchemaElementType, Integer> schemaElementTypeCount,
            SchemaElementType schemaElementType) {
        if (schemaElementTypeCount.containsKey(schemaElementType)) {
            return schemaElementTypeCount.get(schemaElementType);
        }
        return 0;
    }

    private boolean isMatch(QueryMatchOption queryMatchOption, int count) {
        // check if required but empty
        if (queryMatchOption.getSchemaElementOption().equals(QueryMatchOption.OptionType.REQUIRED)
                && count <= 0) {
            return false;
        }
        if (queryMatchOption.getRequireNumberType()
                .equals(QueryMatchOption.RequireNumberType.AT_LEAST)
                && count < queryMatchOption.getRequireNumber()) {
            return false;
        }
        if (queryMatchOption.getRequireNumberType()
                .equals(QueryMatchOption.RequireNumberType.AT_MOST)
                && count > queryMatchOption.getRequireNumber()) {
            return false;
        }
        return true;
    }
}
