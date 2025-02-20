package com.tencent.supersonic.headless.chat.parser.llm;

import com.google.common.collect.Lists;
import com.tencent.supersonic.common.pojo.Text2SQLExemplar;
import com.tencent.supersonic.common.pojo.enums.DataFormatTypeEnum;
import com.tencent.supersonic.common.pojo.enums.EngineType;
import com.tencent.supersonic.common.service.ExemplarService;
import com.tencent.supersonic.common.util.StringUtil;
import com.tencent.supersonic.headless.chat.parser.ParserConfig;
import com.tencent.supersonic.headless.chat.query.llm.s2sql.LLMReq;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.tencent.supersonic.headless.chat.parser.ParserConfig.*;

@Component
@Slf4j
public class PromptHelper {

    @Autowired
    private ParserConfig parserConfig;

    @Autowired
    private ExemplarService exemplarService;

    public List<List<Text2SQLExemplar>> getFewShotExemplars(LLMReq llmReq) {
        int exemplarRecallNumber =
                Integer.valueOf(parserConfig.getParameterValue(PARSER_EXEMPLAR_RECALL_NUMBER));
        int fewShotNumber = Integer.valueOf(parserConfig.getParameterValue(PARSER_FEW_SHOT_NUMBER));
        int selfConsistencyNumber =
                Integer.valueOf(parserConfig.getParameterValue(PARSER_SELF_CONSISTENCY_NUMBER));

        List<Text2SQLExemplar> exemplars = Lists.newArrayList();
        llmReq.getDynamicExemplars().stream().forEach(e -> {
            exemplars.add(e);
        });

        int recallSize = exemplarRecallNumber - llmReq.getDynamicExemplars().size();
        if (recallSize > 0) {
            exemplars.addAll(exemplarService.recallExemplars(llmReq.getQueryText(), recallSize));
        }

        List<List<Text2SQLExemplar>> results = new ArrayList<>();
        // use random collection of exemplars for each self-consistency inference
        for (int i = 0; i < selfConsistencyNumber; i++) {
            List<Text2SQLExemplar> shuffledList = new ArrayList<>(exemplars);
            Collections.shuffle(shuffledList);
            results.add(shuffledList.subList(0, Math.min(shuffledList.size(), fewShotNumber)));
        }

        return results;
    }

    public String buildSideInformation(LLMReq llmReq) {
        String currentDate = llmReq.getCurrentDate();
        List<String> sideInfos = Lists.newArrayList();
        sideInfos.add(String.format("CurrentDate=[%s]", currentDate));

        if (StringUtils.isNotEmpty(llmReq.getPriorExts())) {
            sideInfos.add(String.format("PriorKnowledge=[%s]", llmReq.getPriorExts()));
        }

        LLMReq.LLMSchema schema = llmReq.getSchema();
        if (!isSupportWith(schema.getDatabaseType(), schema.getDatabaseVersion())) {
            sideInfos.add("[Database does not support with statement]");
        }

        String termStr = buildTermStr(llmReq);
        if (StringUtils.isNotEmpty(termStr)) {
            sideInfos.add(String.format("DomainTerms=[%s]", termStr));
        }

        return String.join(",", sideInfos);
    }

    public String buildSchemaStr(LLMReq llmReq) {
        String tableStr = llmReq.getSchema().getDataSetName();

        List<String> metrics = Lists.newArrayList();
        llmReq.getSchema().getMetrics().stream().forEach(metric -> {
            StringBuilder metricStr = new StringBuilder();
            metricStr.append("<");
            metricStr.append(metric.getName());
            if (!CollectionUtils.isEmpty(metric.getAlias())) {
                StringBuilder alias = new StringBuilder();
                metric.getAlias().stream().forEach(a -> alias.append(a + ","));
                metricStr.append(" ALIAS '" + alias + "'");
            }
            if (StringUtils.isNotEmpty(metric.getDataFormatType())) {
                String dataFormatType = metric.getDataFormatType();
                if (DataFormatTypeEnum.DECIMAL.getName().equalsIgnoreCase(dataFormatType)
                        || DataFormatTypeEnum.PERCENT.getName().equalsIgnoreCase(dataFormatType)) {
                    metricStr.append(" FORMAT '" + dataFormatType + "'");
                }
            }
            if (StringUtils.isNotEmpty(metric.getDescription())) {
                metricStr.append(" COMMENT '" + metric.getDescription() + "'");
            }
            if (StringUtils.isNotEmpty(metric.getDefaultAgg())) {
                metricStr.append(" AGGREGATE '" + metric.getDefaultAgg().toUpperCase() + "'");
            }
            metricStr.append(">");
            metrics.add(metricStr.toString());
        });

        List<String> dimensions = Lists.newArrayList();
        llmReq.getSchema().getDimensions().stream().forEach(dimension -> {
            StringBuilder dimensionStr = new StringBuilder();
            dimensionStr.append("<");
            dimensionStr.append(dimension.getName());
            if (!CollectionUtils.isEmpty(dimension.getAlias())) {
                StringBuilder alias = new StringBuilder();
                dimension.getAlias().stream().forEach(a -> alias.append(a + ";"));
                dimensionStr.append(" ALIAS '" + alias + "'");
            }
            if (StringUtils.isNotEmpty(dimension.getTimeFormat())) {
                dimensionStr.append(" FORMAT '" + dimension.getTimeFormat() + "'");
            }
            if (StringUtils.isNotEmpty(dimension.getDescription())) {
                dimensionStr.append(" COMMENT '" + dimension.getDescription() + "'");
            }
            dimensionStr.append(">");
            dimensions.add(dimensionStr.toString());
        });

        List<String> values = Lists.newArrayList();
        llmReq.getSchema().getValues().stream().forEach(value -> {
            StringBuilder valueStr = new StringBuilder();
            String fieldName = value.getFieldName();
            String fieldValue = value.getFieldValue();
            valueStr.append(String.format("<%s='%s'>", fieldName, fieldValue));
            values.add(valueStr.toString());
        });

        String partitionTimeStr = "";
        if (llmReq.getSchema().getPartitionTime() != null) {
            partitionTimeStr =
                    String.format("%s FORMAT '%s'", llmReq.getSchema().getPartitionTime().getName(),
                            llmReq.getSchema().getPartitionTime().getTimeFormat());
        }

        String primaryKeyStr = "";
        if (llmReq.getSchema().getPrimaryKey() != null) {
            primaryKeyStr = String.format("%s", llmReq.getSchema().getPrimaryKey().getName());
        }

        String databaseTypeStr = "";
        if (llmReq.getSchema().getDatabaseType() != null) {
            databaseTypeStr = llmReq.getSchema().getDatabaseType();
        }
        String databaseVersionStr = "";
        if (llmReq.getSchema().getDatabaseVersion() != null) {
            databaseVersionStr = llmReq.getSchema().getDatabaseVersion();
        }

        String template =
                "DatabaseType=[%s], DatabaseVersion=[%s], Table=[%s], PartitionTimeField=[%s], PrimaryKeyField=[%s], "
                        + "Metrics=[%s], Dimensions=[%s], Values=[%s]";
        return String.format(template, databaseTypeStr, databaseVersionStr, tableStr,
                partitionTimeStr, primaryKeyStr, String.join(",", metrics),
                String.join(",", dimensions), String.join(",", values));
    }

    private String buildTermStr(LLMReq llmReq) {
        List<LLMReq.Term> terms = llmReq.getTerms();
        List<String> termStr = Lists.newArrayList();
        terms.stream().forEach(term -> {
            StringBuilder termsDesc = new StringBuilder();
            String description = term.getDescription();
            termsDesc.append(String.format("<%s COMMENT '%s'>", term.getName(), description));
            termStr.add(termsDesc.toString());
        });
        String ret = "";
        if (termStr.size() > 0) {
            ret = String.join(",", termStr);
        }

        return ret;
    }

    public static boolean isSupportWith(String type, String version) {
        if (type.equalsIgnoreCase(EngineType.MYSQL.getName()) && Objects.nonNull(version)
                && StringUtil.compareVersion(version, "8.0") < 0) {
            return false;
        }
        if (type.equalsIgnoreCase(EngineType.CLICKHOUSE.getName()) && Objects.nonNull(version)
                && StringUtil.compareVersion(version, "20.4") < 0) {
            return false;
        }
        return true;
    }
}
