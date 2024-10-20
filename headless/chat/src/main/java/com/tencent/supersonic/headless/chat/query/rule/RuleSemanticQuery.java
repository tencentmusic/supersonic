package com.tencent.supersonic.headless.chat.query.rule;

import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.pojo.enums.FilterOperatorEnum;
import com.tencent.supersonic.headless.api.pojo.DataSetSchema;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.headless.api.pojo.SchemaElementType;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.SemanticSchema;
import com.tencent.supersonic.headless.api.pojo.request.QueryFilter;
import com.tencent.supersonic.headless.api.pojo.request.QueryMultiStructReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryStructReq;
import com.tencent.supersonic.headless.api.pojo.request.SemanticQueryReq;
import com.tencent.supersonic.headless.chat.ChatQueryContext;
import com.tencent.supersonic.headless.chat.query.BaseSemanticQuery;
import com.tencent.supersonic.headless.chat.query.QueryManager;
import com.tencent.supersonic.headless.chat.utils.QueryReqBuilder;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@ToString
public abstract class RuleSemanticQuery extends BaseSemanticQuery {

    protected QueryMatcher queryMatcher = new QueryMatcher();

    public RuleSemanticQuery() {
        QueryManager.register(this);
    }

    public List<SchemaElementMatch> match(List<SchemaElementMatch> candidateElementMatches,
            ChatQueryContext queryCtx) {
        return queryMatcher.match(candidateElementMatches);
    }

    @Override
    public void initS2Sql(DataSetSchema dataSetSchema, User user) {
        initS2SqlByStruct(dataSetSchema);
    }

    public void fillParseInfo(ChatQueryContext chatQueryContext) {
        parseInfo.setQueryMode(getQueryMode());
        SemanticSchema semanticSchema = chatQueryContext.getSemanticSchema();

        fillSchemaElement(parseInfo, semanticSchema);
        fillScore(parseInfo);
        fillDateConfByInherited(parseInfo, chatQueryContext);
    }

    public boolean needFillDateConf(ChatQueryContext chatQueryContext) {
        Long dataSetId = parseInfo.getDataSetId();
        if (Objects.isNull(dataSetId) || dataSetId <= 0L) {
            return false;
        }
        return chatQueryContext.containsPartitionDimensions(dataSetId);
    }

    private void fillDateConfByInherited(SemanticParseInfo queryParseInfo,
            ChatQueryContext chatQueryContext) {
        SemanticParseInfo contextParseInfo = chatQueryContext.getContextParseInfo();
        if (queryParseInfo.getDateInfo() != null || contextParseInfo.getDateInfo() == null
                || needFillDateConf(chatQueryContext)) {
            return;
        }

        if ((QueryManager.isDetailQuery(queryParseInfo.getQueryMode())
                && QueryManager.isDetailQuery(contextParseInfo.getQueryMode()))
                || (QueryManager.isMetricQuery(queryParseInfo.getQueryMode())
                        && QueryManager.isMetricQuery(contextParseInfo.getQueryMode()))) {
            // inherit date info from context
            queryParseInfo.setDateInfo(contextParseInfo.getDateInfo());
            queryParseInfo.getDateInfo().setInherited(true);
        }
    }

    private void fillScore(SemanticParseInfo parseInfo) {
        double totalScore = 0;

        Map<SchemaElementType, SchemaElementMatch> maxSimilarityMatch = new HashMap<>();
        for (SchemaElementMatch match : parseInfo.getElementMatches()) {
            SchemaElementType type = match.getElement().getType();
            if (!maxSimilarityMatch.containsKey(type)
                    || match.getSimilarity() > maxSimilarityMatch.get(type).getSimilarity()) {
                maxSimilarityMatch.put(type, match);
            }
        }

        for (SchemaElementMatch match : maxSimilarityMatch.values()) {
            totalScore += match.getDetectWord().length() * match.getSimilarity();
        }

        parseInfo.setScore(parseInfo.getScore() + totalScore);
    }

    private void fillSchemaElement(SemanticParseInfo parseInfo, SemanticSchema semanticSchema) {
        Set<Long> dataSetIds =
                parseInfo.getElementMatches().stream().map(SchemaElementMatch::getElement)
                        .map(SchemaElement::getDataSetId).collect(Collectors.toSet());
        Long dataSetId = dataSetIds.iterator().next();
        parseInfo.setDataSet(semanticSchema.getDataSet(dataSetId));
        parseInfo.setQueryConfig(semanticSchema.getQueryConfig(dataSetId));
        Map<Long, List<SchemaElementMatch>> dim2Values = new HashMap<>();
        Map<Long, List<SchemaElementMatch>> id2Values = new HashMap<>();

        for (SchemaElementMatch schemaMatch : parseInfo.getElementMatches()) {
            SchemaElement element = schemaMatch.getElement();
            element.setOrder(1 - schemaMatch.getSimilarity());
            switch (element.getType()) {
                case ID:
                    SchemaElement entityElement =
                            semanticSchema.getElement(SchemaElementType.ENTITY, element.getId());
                    if (entityElement != null) {
                        if (id2Values.containsKey(element.getId())) {
                            id2Values.get(element.getId()).add(schemaMatch);
                        } else {
                            id2Values.put(element.getId(),
                                    new ArrayList<>(Arrays.asList(schemaMatch)));
                        }
                    }
                    break;
                case VALUE:
                    SchemaElement dimElement =
                            semanticSchema.getElement(SchemaElementType.DIMENSION, element.getId());
                    if (dimElement != null) {
                        if (dim2Values.containsKey(element.getId())) {
                            dim2Values.get(element.getId()).add(schemaMatch);
                        } else {
                            dim2Values.put(element.getId(),
                                    new ArrayList<>(Arrays.asList(schemaMatch)));
                        }
                    }
                    break;
                case DIMENSION:
                    parseInfo.getDimensions().add(element);
                    break;
                case METRIC:
                    parseInfo.getMetrics().add(element);
                    break;
                case ENTITY:
                    parseInfo.setEntity(element);
                    break;
                default:
            }
        }
        addToFilters(id2Values, parseInfo, semanticSchema, SchemaElementType.ENTITY);
        addToFilters(dim2Values, parseInfo, semanticSchema, SchemaElementType.DIMENSION);
    }

    private void addToFilters(Map<Long, List<SchemaElementMatch>> id2Values,
            SemanticParseInfo parseInfo, SemanticSchema semanticSchema, SchemaElementType entity) {
        if (id2Values == null || id2Values.isEmpty()) {
            return;
        }
        for (Entry<Long, List<SchemaElementMatch>> entry : id2Values.entrySet()) {
            SchemaElement dimension = semanticSchema.getElement(entity, entry.getKey());
            if (dimension.isPartitionTime()) {
                continue;
            }
            if (entry.getValue().size() == 1) {
                SchemaElementMatch schemaMatch = entry.getValue().get(0);
                QueryFilter dimensionFilter = new QueryFilter();
                dimensionFilter.setValue(schemaMatch.getWord());
                dimensionFilter.setBizName(dimension.getBizName());
                dimensionFilter.setName(dimension.getName());
                dimensionFilter.setOperator(FilterOperatorEnum.EQUALS);
                dimensionFilter.setElementID(schemaMatch.getElement().getId());
                parseInfo.setEntity(
                        semanticSchema.getElement(SchemaElementType.ENTITY, entry.getKey()));
                parseInfo.getDimensionFilters().add(dimensionFilter);
            } else {
                QueryFilter dimensionFilter = new QueryFilter();
                List<String> values = new ArrayList<>();
                entry.getValue().stream().forEach(i -> values.add(i.getWord()));
                dimensionFilter.setValue(values);
                dimensionFilter.setBizName(dimension.getBizName());
                dimensionFilter.setName(dimension.getName());
                dimensionFilter.setOperator(FilterOperatorEnum.IN);
                dimensionFilter.setElementID(entry.getKey());
                parseInfo.getDimensionFilters().add(dimensionFilter);
            }
        }
    }

    protected boolean isMultiStructQuery() {
        return false;
    }

    public SemanticQueryReq multiStructExecute() {
        String queryMode = parseInfo.getQueryMode();

        if (parseInfo.getDataSetId() != null || StringUtils.isEmpty(queryMode)
                || !QueryManager.containsRuleQuery(queryMode)) {
            // reach here some error may happen
            log.error("not find QueryMode");
            throw new RuntimeException("not find QueryMode");
        }

        return convertQueryMultiStruct();
    }

    @Override
    public void setParseInfo(SemanticParseInfo parseInfo) {
        this.parseInfo = parseInfo;
    }

    public static List<RuleSemanticQuery> resolve(Long dataSetId,
            List<SchemaElementMatch> candidateElementMatches, ChatQueryContext chatQueryContext) {
        List<RuleSemanticQuery> matchedQueries = new ArrayList<>();
        for (RuleSemanticQuery semanticQuery : QueryManager.getRuleQueries()) {
            List<SchemaElementMatch> matches =
                    semanticQuery.match(candidateElementMatches, chatQueryContext);

            if (matches.size() > 0) {
                RuleSemanticQuery query =
                        QueryManager.createRuleQuery(semanticQuery.getQueryMode());
                query.getParseInfo().getElementMatches().addAll(matches);
                matchedQueries.add(query);
            }
        }
        return matchedQueries;
    }

    protected QueryStructReq convertQueryStruct() {
        return QueryReqBuilder.buildStructReq(parseInfo);
    }

    protected QueryMultiStructReq convertQueryMultiStruct() {
        return QueryReqBuilder.buildMultiStructReq(parseInfo);
    }
}
