package com.tencent.supersonic.chat.query.plugin.dsl;

import static java.time.LocalDate.now;

import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.SemanticSchema;
import com.tencent.supersonic.chat.api.pojo.request.QueryFilter;
import com.tencent.supersonic.chat.api.pojo.request.QueryFilters;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.calcite.SqlParseUtils;
import com.tencent.supersonic.common.util.calcite.SqlParserInfo;
import com.tencent.supersonic.knowledge.service.SchemaService;
import com.tencent.supersonic.semantic.api.model.enums.TimeDimensionEnum;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class DSLBuilder {

    public static final String COMMA_WRAPPER = "'%s'";
    public static final String SPACE_WRAPPER = " %s ";
    protected static final String SUB_TABLE = " ( select * from  t_{0} where {1} >= ''{2}'' and  {1} <= ''{3}'' {4} ) as  t_sub_{0}";

    public String build(QueryFilters queryFilters, SemanticParseInfo parseInfo, LLMResp llmResp, Long domainId)
            throws SqlParseException {

        String sqlOutput = llmResp.getSqlOutput();
        String domainName = llmResp.getDomainName();

        // 1. extra deal with,such as add alias.
        sqlOutput = extraConvert(sqlOutput, domainId);

        SqlParserInfo sqlParseInfo = SqlParseUtils.getSqlParseInfo(sqlOutput);

        String tableName = sqlParseInfo.getTableName();
        List<String> allFields = sqlParseInfo.getAllFields();

        if (StringUtils.isEmpty(domainName)) {
            domainName = tableName;
        }

        // 2. replace the llm dsl, such as replace fieldName and tableName.
        log.info("sqlParseInfo:{} ,domainName:{},domainId:{}", sqlParseInfo, domainName, domainId);

        SemanticSchema semanticSchema = ContextUtils.getBean(SchemaService.class).getSemanticSchema();
        List<SchemaElement> dbAllFields = new ArrayList<>();
        dbAllFields.addAll(semanticSchema.getMetrics());
        dbAllFields.addAll(semanticSchema.getDimensions());

        Map<String, String> fieldToBizName = getMapInfo(domainId, dbAllFields);

        for (String fieldName : allFields) {
            String fieldBizName = fieldToBizName.get(fieldName);
            if (StringUtils.isNotEmpty(fieldBizName)) {
                sqlOutput = sqlOutput.replaceAll(fieldName, fieldBizName);
            }
        }
        //3. deal with dayNo.
        DateConf dateInfo = new DateConf();
        if (Objects.nonNull(parseInfo) && Objects.nonNull(parseInfo.getDateInfo())) {
            dateInfo = parseInfo.getDateInfo();
        } else {
            String startDate = now().plusDays(-4).toString();
            String endDate = now().plusDays(-4).toString();
            dateInfo.setStartDate(startDate);
            dateInfo.setEndDate(endDate);
        }

        String startDate = dateInfo.getStartDate();
        String endDate = dateInfo.getEndDate();
        String period = dateInfo.getPeriod();
        TimeDimensionEnum timeDimension = TimeDimensionEnum.valueOf(period);
        String dayField = timeDimension.getName();

        String queryFilter = getQueryFilter(queryFilters);

        String subTable = MessageFormat.format(SUB_TABLE, domainId, dayField, startDate, endDate, queryFilter);
        String querySql = sqlOutput.replaceAll(tableName, subTable);

        log.info("querySql:{},sqlOutput:{},dateInfo:{}", querySql, sqlOutput, dateInfo);
        return querySql;
    }

    private String getQueryFilter(QueryFilters queryFilters) {
        String queryFilter = "";
        if (Objects.isNull(queryFilters) || CollectionUtils.isEmpty(queryFilters.getFilters())) {
            return queryFilter;
        }
        List<QueryFilter> filters = queryFilters.getFilters();
        for (QueryFilter filter : filters) {
            queryFilter = getSpaceWrap(queryFilter) + "and" + getSpaceWrap(filter.getBizName()) + getSpaceWrap(
                    filter.getOperator().getValue()) + getCommaWrap(filter.getValue().toString());
        }
        return queryFilter;
    }

    protected String extraConvert(String sqlOutput, Long domainId) throws SqlParseException {
        return SqlParseUtils.addAliasToSql(sqlOutput);
    }

    protected Map<String, String> getMapInfo(Long domainId, List<SchemaElement> metrics) {
        return metrics.stream().filter(entry -> entry.getDomain().equals(domainId))
                .collect(Collectors.toMap(SchemaElement::getName, a -> a.getBizName(), (k1, k2) -> k1));
    }


    private String getCommaWrap(String value) {
        return String.format(COMMA_WRAPPER, value);
    }

    private String getSpaceWrap(String value) {
        return String.format(SPACE_WRAPPER, value);
    }
}
