package com.tencent.supersonic.chat.domain.utils;

import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.Filter;
import com.tencent.supersonic.chat.api.pojo.QueryMatchInfo;
import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.chat.api.pojo.SchemaElementType;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.component.SemanticQuery;
import com.tencent.supersonic.semantic.api.core.response.DimSchemaResp;
import com.tencent.supersonic.chat.domain.pojo.config.ChatConfigRichInfo;
import com.tencent.supersonic.common.pojo.SchemaItem;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class ContextHelper {

    public static Comparator<Map.Entry<Integer, QueryMatchInfo>> DomainStatComparator
            = (o1, o2) -> domainSchemaElementCountComparator(o1.getValue(), o2.getValue());
    public static Comparator<Map.Entry<SemanticQuery, QueryMatchInfo>> SemanticQueryStatComparator
            = (o1, o2) -> domainSchemaElementCountComparator(o1.getValue(), o2.getValue());
    /**
     * similarity desc
     */
    public static Comparator<SchemaElementMatch> schemaElementMatchComparatorBySimilarity
            = new Comparator<SchemaElementMatch>() {
        @Override
        public int compare(SchemaElementMatch o1, SchemaElementMatch o2) {
            return (int) ((o2.getSimilarity() - o1.getSimilarity()) * 100);
        }
    };

    public static void updateDomain(SemanticParseInfo from, SemanticParseInfo to) {
        if (from != null && from.getDomainId() != null) {
            to.setDomainId(from.getDomainId());
        }
        if (from != null && from.getDomainName() != null) {
            to.setDomainName(from.getDomainName());
        }
    }

    public static void updateEntity(SemanticParseInfo from, SemanticParseInfo to) {
        if (from != null && from.getEntity() != null && from.getEntity() > 0) {
            to.setEntity(from.getEntity());
        }
    }

    public static void updateSemanticQuery(SemanticParseInfo from, SemanticParseInfo to) {
        to.setQueryMode(from.getQueryMode());
    }

    /**
     * update time if from is not null
     *
     * @param from
     * @param to
     */
    public static void updateTime(SemanticParseInfo from, SemanticParseInfo to) {
        if (from != null && from.getDateInfo() != null) {
            to.setDateInfo(from.getDateInfo());
        }
    }

    /**
     * update time if time is null and from is not null
     *
     * @param from
     * @param to
     */
    public static void updateTimeIfEmpty(SemanticParseInfo from, SemanticParseInfo to) {
        if (from != null && from.getDateInfo() != null && to.getDateInfo() == null) {
            to.setDateInfo(from.getDateInfo());
        }
    }

    public static void updateDomainIfEmpty(SemanticParseInfo from, SemanticParseInfo to) {
        if (from != null && from.getDomainId() != null && to.getDomainId() == null) {
            to.setDomainId(from.getDomainId());
        }

        if (from != null && from.getDomainName() != null && to.getDomainName() == null) {
            to.setDomainName(from.getDomainName());
        }
    }

    /**
     * add from to list if  list is empty and from is not empty
     *
     * @param from
     * @param to
     */
    public static void addIfEmpty(Set from, Set to) {
        if (to.isEmpty() && !from.isEmpty()) {
            to.addAll(from);
        }
    }

    /***
     * append from to list if from is not empty
     * @param from
     * @param to
     */
    public static void appendList(Set from, Set to) {
        if (!from.isEmpty()) {
            to.addAll(from);
        }
    }

    /**
     * update list if from is not empty
     *
     * @param from
     * @param to
     */
    public static void updateList(Set from, Set to) {
        if (!from.isEmpty()) {
            to.clear();
            to.addAll(from);
        }
    }

    /**
     * count desc > similarity desc
     */
    public static int domainSchemaElementCountComparator(QueryMatchInfo o1, QueryMatchInfo o2) {
        int difference = o1.getCount() - o2.getCount();
        if (difference == 0) {
            return (int) ((o1.getMaxSimilarity() - o2.getMaxSimilarity()) * 100);
        }
        return difference;
    }

    public static void setEntityId(Long dimensionId, String value, ChatConfigRichInfo chaConfigRichDesc,
            SemanticParseInfo semanticParseInfo) {
        if (chaConfigRichDesc != null && chaConfigRichDesc.getEntity() != null) {
            Optional<DimSchemaResp> dimensionDesc = chaConfigRichDesc.getEntity().getEntityIds().stream()
                    .filter(i -> i.getId().equals(dimensionId)).findFirst();
            if (dimensionDesc.isPresent() && StringUtils.isNumeric(value)) {
                semanticParseInfo.setEntity(Long.valueOf(value));
            }
        }
    }

    public static boolean hasEntityId(ChatContext chatCtx) {
        if (chatCtx != null && chatCtx.getParseInfo() != null) {
            return chatCtx.getParseInfo().getEntity() > 0;
        }
        return false;
    }

    /***
     * merge Context SchemaElementMatch
     * @param toSchemaElementMatch
     * @param elementMatches
     * @param schemaElementTypes
     * @param contextSemanticParse
     */
    public static void mergeContextSchemaElementMatch(List<SchemaElementMatch> toSchemaElementMatch,
            List<SchemaElementMatch> elementMatches, List<SchemaElementType> schemaElementTypes,
            SemanticParseInfo contextSemanticParse) {

        SchemaElementMatch domainMatch = SchemaElementMatch.builder()
                .elementType(SchemaElementType.DOMAIN)
                .elementID(contextSemanticParse.getDomainId().intValue())
                .similarity(1.0)
                .word(contextSemanticParse.getDomainName())
                .detectWord(contextSemanticParse.getDomainName())
                .build();
        toSchemaElementMatch.add(domainMatch);

        for (SchemaElementType schemaElementType : schemaElementTypes) {
            switch (schemaElementType) {
                case DIMENSION:
                    if (contextSemanticParse.getDimensions().size() > 0) {
                        for (SchemaItem dimension : contextSemanticParse.getDimensions()) {
                            addSchemaElementMatch(toSchemaElementMatch, elementMatches, SchemaElementType.DIMENSION,
                                    dimension);
                        }
                    }
                    break;
                case METRIC:
                    if (contextSemanticParse.getMetrics().size() > 0) {
                        for (SchemaItem metric : contextSemanticParse.getMetrics()) {
                            addSchemaElementMatch(toSchemaElementMatch, elementMatches, SchemaElementType.METRIC,
                                    metric);
                        }
                    }
                    break;
                case VALUE:
                    if (contextSemanticParse.getDimensionFilters().size() > 0) {
                        for (Filter chatFilter : contextSemanticParse.getDimensionFilters()) {
                            if (!isInSchemaElementMatchList(elementMatches, SchemaElementType.VALUE,
                                    chatFilter.getValue().toString())) {
                                toSchemaElementMatch.add(
                                        getSchemaElementMatchByContext(chatFilter.getElementID().intValue(),
                                                chatFilter.getValue().toString(), SchemaElementType.VALUE));
                            }
                        }
                    }
                    break;
                default:
            }
        }
    }

    /**
     * is that SchemaElementType and word in SchemaElementMatch list
     *
     * @param elementMatches
     * @param schemaElementType
     * @param word
     * @return
     */
    private static boolean isInSchemaElementMatchList(List<SchemaElementMatch> elementMatches,
            SchemaElementType schemaElementType, String word) {
        if (CollectionUtils.isEmpty(elementMatches)) {
            return false;
        }
        Long num = elementMatches.stream()
                .filter(element -> element != null && element.getWord() != null && element.getWord()
                        .equalsIgnoreCase(word) && element.getElementType().equals(schemaElementType)).count();
        return num > 0;
    }

    private static void addSchemaElementMatch(List<SchemaElementMatch> toAddSchemaElementMatch,
            List<SchemaElementMatch> elementMatches, SchemaElementType schemaElementType, SchemaItem schemaItem) {
        if (Objects.isNull(schemaItem) || Objects.isNull(schemaItem.getId()) || Objects.isNull(schemaItem.getName())) {
            return;
        }
        if (!isInSchemaElementMatchList(elementMatches, schemaElementType, schemaItem.getName())) {
            toAddSchemaElementMatch.add(
                    getSchemaElementMatchByContext(schemaItem.getId().intValue(), schemaItem.getName(),
                            schemaElementType));
        }
    }

    private static SchemaElementMatch getSchemaElementMatchByContext(int id, String word,
            SchemaElementType schemaElementType) {
        return SchemaElementMatch.builder()
                .elementID(id)
                .elementType(schemaElementType)
                .word(word)
                .similarity(0.5)
                .build();
    }

}
