package com.tencent.supersonic.headless.chat.parser.llm;

import com.google.common.collect.Lists;
import com.tencent.supersonic.common.pojo.Text2SQLExemplar;
import com.tencent.supersonic.common.pojo.enums.DataFormatTypeEnum;
import com.tencent.supersonic.common.pojo.enums.EngineType;
import com.tencent.supersonic.common.service.ExemplarService;
import com.tencent.supersonic.common.util.StringUtil;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.chat.parser.ParserConfig;
import com.tencent.supersonic.headless.chat.query.llm.s2sql.LLMReq;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tencent.supersonic.common.pojo.DimensionConstants.*;
import static com.tencent.supersonic.headless.chat.parser.ParserConfig.*;

@Component
@Slf4j
@RequiredArgsConstructor
public class PromptHelper {

    private final ParserConfig parserConfig;

    private final ExemplarService exemplarService;

    public List<List<Text2SQLExemplar>> getFewShotExemplars(LLMReq llmReq) {
        int exemplarRecallNumber =
                Integer.parseInt(parserConfig.getParameterValue(PARSER_EXEMPLAR_RECALL_NUMBER));
        int fewShotNumber =
                Integer.parseInt(parserConfig.getParameterValue(PARSER_FEW_SHOT_NUMBER));
        int selfConsistencyNumber =
                Integer.parseInt(parserConfig.getParameterValue(PARSER_SELF_CONSISTENCY_NUMBER));

        // Use only agent-scoped exemplars (from memory_<agentId> collection).
        // Do NOT fall back to global system exemplars — they are generic and
        // cause irrelevant few-shot examples for agent-specific queries.
        List<Text2SQLExemplar> exemplars = Lists.newArrayList(llmReq.getDynamicExemplars());

        if (exemplars.isEmpty()) {
            // No agent-scoped exemplars — auto-generate basic exemplars from the dataset schema
            // so the LLM knows the correct field names and SQL patterns for this agent.
            exemplars = buildSchemaExemplars(llmReq);
        }

        List<List<Text2SQLExemplar>> results = new ArrayList<>();
        if (exemplars.isEmpty()) {
            // Schema has no usable structure — proceed with zero-shot prompts.
            for (int i = 0; i < selfConsistencyNumber; i++) {
                results.add(new ArrayList<>());
            }
            return results;
        }
        // use random collection of exemplars for each self-consistency inference
        for (int i = 0; i < selfConsistencyNumber; i++) {
            List<Text2SQLExemplar> shuffledList = new ArrayList<>(exemplars);
            List<Text2SQLExemplar> same = shuffledList.stream() // 相似度极高的话，先找出来
                    .filter(e -> e.getSimilarity() > 0.989).collect(Collectors.toList());
            List<Text2SQLExemplar> noSame = shuffledList.stream()
                    .filter(e -> e.getSimilarity() <= 0.989).collect(Collectors.toList());
            if ((noSame.size() - same.size()) > fewShotNumber) {// 去除部分最低分
                noSame.sort(Comparator.comparingDouble(Text2SQLExemplar::getSimilarity));
                noSame = noSame.subList((noSame.size() - fewShotNumber) / 2, noSame.size());
            }
            Text2SQLExemplar mostSimilar = noSame.get(noSame.size() - 1);
            Collections.shuffle(noSame);
            List<Text2SQLExemplar> ts;
            if (same.size() > 0) {// 一样的话，必须作为提示语
                ts = new ArrayList<>();
                int needSize = Math.min(noSame.size() + same.size(), fewShotNumber);
                if (needSize > same.size()) {
                    ts.addAll(noSame.subList(0, needSize - same.size()));
                }
                ts.addAll(same);
            } else { // 至少要一个最像的
                ts = noSame.subList(0, Math.min(noSame.size(), fewShotNumber));
                if (!ts.contains(mostSimilar)) {
                    ts.remove(ts.size() - 1);
                    ts.add(mostSimilar);
                }
            }
            results.add(ts);
        }
        return results;
    }

    /**
     * Auto-generate basic few-shot exemplars from the dataset schema when no agent-specific
     * exemplars exist. Produces one aggregate exemplar (GROUP BY date + metric) and one detail
     * exemplar (SELECT dimensions), so the LLM learns the correct field names and SQL patterns.
     */
    private List<Text2SQLExemplar> buildSchemaExemplars(LLMReq llmReq) {
        LLMReq.LLMSchema schema = llmReq.getSchema();
        String dataSetName = schema.getDataSetName();
        String schemaStr = buildSchemaStr(llmReq);
        String sideInfo = buildSideInformation(llmReq);
        List<Text2SQLExemplar> exemplars = new ArrayList<>();

        SchemaElement partitionTime = schema.getPartitionTime();
        List<SchemaElement> metrics = schema.getMetrics();
        List<SchemaElement> dimensions = schema.getDimensions();

        // Exemplar 1: aggregate — partition time + first metric
        if (partitionTime != null && !CollectionUtils.isEmpty(metrics)) {
            SchemaElement metric = metrics.get(0);
            String agg = StringUtils.isNotBlank(metric.getDefaultAgg())
                    ? metric.getDefaultAgg().toUpperCase()
                    : "SUM";
            String question = String.format("最近7天的%s汇总是多少", metric.getName());
            String sql = String.format(
                    "SELECT %s, %s(%s) FROM %s WHERE %s >= '2025-01-01' AND %s <= '2025-01-07' GROUP BY %s",
                    partitionTime.getName(), agg, metric.getName(), dataSetName,
                    partitionTime.getName(), partitionTime.getName(), partitionTime.getName());
            exemplars.add(Text2SQLExemplar.builder().question(question).sideInfo(sideInfo)
                    .dbSchema(schemaStr).sql(sql).build());
        }

        // Exemplar 2: detail — partition time + top 2 non-time dimensions
        if (partitionTime != null && !CollectionUtils.isEmpty(dimensions)) {
            List<String> dimNames =
                    dimensions.stream().filter(d -> !d.getName().equals(partitionTime.getName()))
                            .limit(2).map(SchemaElement::getName).collect(Collectors.toList());
            String selectFields =
                    Stream.concat(Stream.of(partitionTime.getName()), dimNames.stream())
                            .collect(Collectors.joining(", "));
            String question =
                    String.format("查一下%s的明细数据", dimNames.isEmpty() ? dataSetName : dimNames.get(0));
            String sql = String.format(
                    "SELECT %s FROM %s WHERE %s >= '2025-01-01' AND %s <= '2025-01-07' LIMIT 1000",
                    selectFields, dataSetName, partitionTime.getName(), partitionTime.getName());
            exemplars.add(Text2SQLExemplar.builder().question(question).sideInfo(sideInfo)
                    .dbSchema(schemaStr).sql(sql).build());
        }

        log.info("built {} schema-derived exemplars for dataset: {}", exemplars.size(),
                dataSetName);
        return exemplars;
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
        llmReq.getSchema().getMetrics().forEach(metric -> {
            StringBuilder metricStr = new StringBuilder();
            metricStr.append("<");
            metricStr.append(metric.getName());
            if (!CollectionUtils.isEmpty(metric.getAlias())) {
                StringBuilder alias = new StringBuilder();
                metric.getAlias().forEach(a -> alias.append(a).append(","));
                metricStr.append(" ALIAS '").append(alias).append("'");
            }
            if (StringUtils.isNotEmpty(metric.getDataFormatType())) {
                String dataFormatType = metric.getDataFormatType();
                if (DataFormatTypeEnum.DECIMAL.getName().equalsIgnoreCase(dataFormatType)
                        || DataFormatTypeEnum.PERCENT.getName().equalsIgnoreCase(dataFormatType)) {
                    metricStr.append(" FORMAT '").append(dataFormatType).append("'");
                }
            }
            if (StringUtils.isNotEmpty(metric.getDescription())) {
                metricStr.append(" COMMENT '").append(metric.getDescription()).append("'");
            }
            if (StringUtils.isNotEmpty(metric.getDefaultAgg())) {
                metricStr.append(" AGGREGATE '").append(metric.getDefaultAgg().toUpperCase())
                        .append("'");
            }
            metricStr.append(">");
            metrics.add(metricStr.toString());
        });

        List<String> dimensions = Lists.newArrayList();
        llmReq.getSchema().getDimensions().forEach(dimension -> {
            StringBuilder dimensionStr = new StringBuilder();
            dimensionStr.append("<");
            dimensionStr.append(dimension.getName());
            if (!CollectionUtils.isEmpty(dimension.getAlias())) {
                StringBuilder alias = new StringBuilder();
                dimension.getAlias().forEach(a -> alias.append(a).append(";"));
                dimensionStr.append(" ALIAS '").append(alias).append("'");
            }
            if (Objects.nonNull(dimension.getExtInfo().get(DIMENSION_DATA_TYPE))) {
                dimensionStr.append(" DATATYPE '")
                        .append(dimension.getExtInfo().get(DIMENSION_DATA_TYPE)).append("'");
            }
            if (StringUtils.isNotEmpty(dimension.getTimeFormat())) {
                dimensionStr.append(" FORMAT '").append(dimension.getTimeFormat()).append("'");
            }
            if (StringUtils.isNotEmpty(dimension.getDescription())) {
                dimensionStr.append(" COMMENT '").append(dimension.getDescription()).append("'");
            }
            dimensionStr.append(">");
            dimensions.add(dimensionStr.toString());
        });

        List<String> values = Lists.newArrayList();
        List<LLMReq.ElementValue> elementValueList = llmReq.getSchema().getValues();
        if (elementValueList != null) {
            elementValueList.forEach(value -> {
                StringBuilder valueStr = new StringBuilder();
                String fieldName = value.getFieldName();
                String fieldValue = value.getFieldValue();
                valueStr.append(String.format("<%s='%s'>", fieldName, fieldValue));
                values.add(valueStr.toString());
            });
        }

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
        terms.forEach(term -> {
            StringBuilder termsDesc = new StringBuilder();
            String description = term.getDescription();
            termsDesc.append(String.format("<%s COMMENT '%s'>", term.getName(), description));
            termStr.add(termsDesc.toString());
        });
        String ret = "";
        if (!termStr.isEmpty()) {
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
