package com.tencent.supersonic.headless.chat.query.rule;

import com.tencent.supersonic.common.pojo.Aggregator;
import com.tencent.supersonic.common.pojo.Filter;
import com.tencent.supersonic.common.pojo.Order;
import com.tencent.supersonic.common.pojo.enums.FilterOperatorEnum;
import com.tencent.supersonic.headless.api.pojo.*;
import com.tencent.supersonic.headless.api.pojo.request.*;
import com.tencent.supersonic.headless.chat.ChatQueryContext;
import com.tencent.supersonic.headless.chat.query.BaseSemanticQuery;
import com.tencent.supersonic.headless.chat.query.QueryManager;
import com.tencent.supersonic.headless.chat.utils.QueryReqBuilder;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static com.tencent.supersonic.headless.api.pojo.SchemaElementType.TERM;
import static com.tencent.supersonic.headless.chat.query.rule.QueryMatchOption.OptionType.OPTIONAL;
import static com.tencent.supersonic.headless.chat.query.rule.QueryMatchOption.RequireNumberType.AT_LEAST;

@Slf4j
@ToString
public abstract class RuleSemanticQuery extends BaseSemanticQuery {

    protected QueryMatcher queryMatcher = new QueryMatcher();

    public RuleSemanticQuery() {
        QueryManager.register(this);
        queryMatcher.addOption(TERM, OPTIONAL, AT_LEAST, 0);
    }

    public List<SchemaElementMatch> match(List<SchemaElementMatch> candidateElementMatches,
            ChatQueryContext queryCtx) {
        return queryMatcher.match(candidateElementMatches);
    }

    @Override
    public void buildS2Sql(DataSetSchema dataSetSchema) {
        QueryStructReq queryStructReq = convertQueryStruct();
        convertBizNameToName(dataSetSchema, queryStructReq);
        QuerySqlReq querySQLReq = queryStructReq.convert();
        parseInfo.getSqlInfo().setParsedS2SQL(querySQLReq.getSql());
        parseInfo.getSqlInfo().setCorrectedS2SQL(querySQLReq.getSql());
    }

    protected QueryStructReq convertQueryStruct() {
        return QueryReqBuilder.buildStructReq(parseInfo);
    }

    protected void fillParseInfo(ChatQueryContext chatQueryContext, Long dataSetId) {
        SemanticSchema semanticSchema = chatQueryContext.getSemanticSchema();

        parseInfo.setQueryMode(getQueryMode());
        parseInfo.setDataSet(semanticSchema.getDataSet(dataSetId));
        parseInfo.setQueryConfig(semanticSchema.getQueryConfig(dataSetId));
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
        SemanticParseInfo contextParseInfo = chatQueryContext.getRequest().getContextParseInfo();
        if (queryParseInfo.getDateInfo() != null || Objects.isNull(contextParseInfo)
                || Objects.isNull(contextParseInfo.getDateInfo())
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

        Map<Long, List<SchemaElementMatch>> dim2Values = new HashMap<>();

        for (SchemaElementMatch schemaMatch : parseInfo.getElementMatches()) {
            SchemaElement element = schemaMatch.getElement();
            element.setOrder(1 - schemaMatch.getSimilarity());
            switch (element.getType()) {
                case VALUE:
                    SchemaElement dimElement =
                            semanticSchema.getElement(SchemaElementType.DIMENSION, element.getId());
                    if (dimElement != null) {
                        if (dim2Values.containsKey(element.getId())) {
                            dim2Values.get(element.getId()).add(schemaMatch);
                        } else {
                            dim2Values.put(element.getId(),
                                    new ArrayList<>(Collections.singletonList(schemaMatch)));
                        }
                    }
                    break;
                case DIMENSION:
                    parseInfo.getDimensions().add(element);
                    break;
                case METRIC:
                    parseInfo.getMetrics().add(element);
                    break;
                default:
            }
        }
        addToFilters(dim2Values, parseInfo, semanticSchema, SchemaElementType.DIMENSION);
    }

    private void addToFilters(Map<Long, List<SchemaElementMatch>> id2Values,
            SemanticParseInfo parseInfo, SemanticSchema semanticSchema,
            SchemaElementType elementType) {
        if (id2Values == null || id2Values.isEmpty()) {
            return;
        }
        for (Entry<Long, List<SchemaElementMatch>> entry : id2Values.entrySet()) {
            SchemaElement dimension = semanticSchema.getElement(elementType, entry.getKey());
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
                parseInfo.getDimensionFilters().add(dimensionFilter);
            } else {
                QueryFilter dimensionFilter = new QueryFilter();
                List<String> values = new ArrayList<>();
                entry.getValue().forEach(i -> values.add(i.getWord()));
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

    public static List<RuleSemanticQuery> resolve(Long dataSetId,
            List<SchemaElementMatch> candidateElementMatches, ChatQueryContext chatQueryContext) {
        List<RuleSemanticQuery> matchedQueries = new ArrayList<>();

        for (RuleSemanticQuery semanticQuery : QueryManager.getRuleQueries()) {
            List<SchemaElementMatch> matches =
                    semanticQuery.match(candidateElementMatches, chatQueryContext);
            if (!matches.isEmpty()) {
                RuleSemanticQuery query =
                        QueryManager.createRuleQuery(semanticQuery.getQueryMode());
                query.getParseInfo().getElementMatches().addAll(matches);
                query.fillParseInfo(chatQueryContext, dataSetId);
                matchedQueries.add(query);
            }
        }
        return matchedQueries;
    }

    protected QueryMultiStructReq convertQueryMultiStruct() {
        return QueryReqBuilder.buildMultiStructReq(parseInfo);
    }


    protected void convertBizNameToName(DataSetSchema dataSetSchema,
            QueryStructReq queryStructReq) {
        Map<String, String> bizNameToName = dataSetSchema.getBizNameToName();
        List<Order> orders = queryStructReq.getOrders();
        if (CollectionUtils.isNotEmpty(orders)) {
            for (Order order : orders) {
                order.setColumn(bizNameToName.get(order.getColumn()));
            }
        }
        List<Aggregator> aggregators = queryStructReq.getAggregators();
        if (CollectionUtils.isNotEmpty(aggregators)) {
            for (Aggregator aggregator : aggregators) {
                aggregator.setColumn(bizNameToName.get(aggregator.getColumn()));
            }
        }
        List<String> groups = queryStructReq.getGroups();
        if (CollectionUtils.isNotEmpty(groups)) {
            groups = groups.stream().map(bizNameToName::get).collect(Collectors.toList());
            queryStructReq.setGroups(groups);
        }
        List<Filter> dimensionFilters = queryStructReq.getDimensionFilters();
        if (CollectionUtils.isNotEmpty(dimensionFilters)) {
            dimensionFilters
                    .forEach(filter -> filter.setName(bizNameToName.get(filter.getBizName())));
        }
        List<Filter> metricFilters = queryStructReq.getMetricFilters();
        if (CollectionUtils.isNotEmpty(metricFilters)) {
            metricFilters.forEach(filter -> filter.setName(bizNameToName.get(filter.getBizName())));
        }
    }

}
