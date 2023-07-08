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
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DslToSemanticInfo {

    public static final String SUB_TABLE = " ( select * from  t_{0} where {1} >= ''{2}'' and  {1} <= ''{3}'' ) as  t_sub_{0}";
    private static final Logger LOG = LoggerFactory.getLogger(DslToSemanticInfo.class);

    public static String convert(SemanticParseInfo parseInfo, LLMResp llmResp) {

        String sqlOutput = llmResp.getSqlOutput();
        String domainName = llmResp.getDomainName();

        DomainInfos domainInfos = ContextUtils.getBean(WordNatureService.class).getCache().getUnchecked("");

        SqlParserInfo sqlParseInfo = SqlParseUtils.getSqlParseInfo(sqlOutput);
        String tableName = sqlParseInfo.getTableName();
        if (StringUtils.isEmpty(domainName)) {
            domainName = tableName;
        }

        List<String> allFields = sqlParseInfo.getAllFields();
        Map<String, Integer> domainNameToId = domainInfos.getDomains().stream()
                .collect(Collectors.toMap(ItemDO::getName, a -> a.getDomain(), (k1, k2) -> k1));

        Integer domainId = domainNameToId.get(domainName);
        LOG.info("sqlParseInfo:{} ,domainName:{},domainId:{}", sqlParseInfo, domainName, domainId);

        List<ItemDO> fieldList = domainInfos.getMetrics();
        fieldList.addAll(domainInfos.getDimensions());
        Map<String, String> fieldToBizName = getMapInfo(domainId, fieldList);

        for (String fieldName : allFields) {
            String fieldBizName = fieldToBizName.get(fieldName);
            if (StringUtils.isNotEmpty(fieldBizName)) {
                sqlOutput = sqlOutput.replaceAll(fieldName, fieldBizName);
            }
        }
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

        // add dayno
        String subTable = MessageFormat.format(SUB_TABLE, domainId, dayField, startDate, endDate);
        String querySql = sqlOutput.replaceAll(tableName, subTable);

        LOG.info("querySql:{},sqlOutput:{},dateInfo:{}", querySql, sqlOutput, dateInfo);
        return querySql;
    }

    private static Map<String, String> getMapInfo(Integer domainId, List<ItemDO> metrics) {
        return metrics.stream().filter(entry -> entry.getDomain().equals(domainId))
                .collect(Collectors.toMap(ItemDO::getName, a -> a.getBizName(), (k1, k2) -> k1));
    }


}
