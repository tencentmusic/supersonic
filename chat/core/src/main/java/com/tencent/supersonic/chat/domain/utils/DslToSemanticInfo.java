package com.tencent.supersonic.chat.domain.utils;

import static java.time.LocalDate.now;

import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.application.knowledge.WordNatureService;
import com.tencent.supersonic.chat.domain.pojo.chat.DomainInfos;
import com.tencent.supersonic.chat.domain.pojo.chat.LLMResp;
import com.tencent.supersonic.common.nlp.ItemDO;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.util.calcite.SqlParseUtils;
import com.tencent.supersonic.common.util.calcite.SqlParserInfo;
import com.tencent.supersonic.common.util.context.ContextUtils;
import com.tencent.supersonic.semantic.api.core.enums.TimeDimensionEnum;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class DslToSemanticInfo {

    protected static final String SUB_TABLE = " ( select * from  t_{0} where {1} >= ''{2}'' and  {1} <= ''{3}'' ) as  t_sub_{0}";

    public String convert(SemanticParseInfo parseInfo, LLMResp llmResp, Integer domainId) throws SqlParseException {

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

        DomainInfos domainInfos = ContextUtils.getBean(WordNatureService.class).getCache().getUnchecked("");
        List<ItemDO> fieldList = domainInfos.getMetrics();
        fieldList.addAll(domainInfos.getDimensions());
        Map<String, String> fieldToBizName = getMapInfo(domainId, fieldList);

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

        String subTable = MessageFormat.format(SUB_TABLE, domainId, dayField, startDate, endDate);
        String querySql = sqlOutput.replaceAll(tableName, subTable);

        log.info("querySql:{},sqlOutput:{},dateInfo:{}", querySql, sqlOutput, dateInfo);
        return querySql;
    }

    protected String extraConvert(String sqlOutput, Integer domainId) throws SqlParseException {
        return SqlParseUtils.addAliasToSql(sqlOutput);
    }

    protected Map<String, String> getMapInfo(Integer domainId, List<ItemDO> metrics) {
        return metrics.stream().filter(entry -> entry.getDomain().equals(domainId))
                .collect(Collectors.toMap(ItemDO::getName, a -> a.getBizName(), (k1, k2) -> k1));
    }


}
