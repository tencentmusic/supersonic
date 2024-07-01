package com.tencent.supersonic.headless.chat.parser.llm;

import com.google.common.collect.Lists;
import com.tencent.supersonic.common.pojo.SqlExemplar;
import com.tencent.supersonic.common.service.ExemplarService;
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

import static com.tencent.supersonic.headless.chat.parser.ParserConfig.PARSER_EXEMPLAR_RECALL_NUMBER;
import static com.tencent.supersonic.headless.chat.parser.ParserConfig.PARSER_FEW_SHOT_NUMBER;
import static com.tencent.supersonic.headless.chat.parser.ParserConfig.PARSER_SELF_CONSISTENCY_NUMBER;

@Component
@Slf4j
public class PromptHelper {

    @Autowired
    private ParserConfig parserConfig;

    @Autowired
    private ExemplarService exemplarService;

    public List<List<SqlExemplar>> getFewShotExemplars(LLMReq llmReq) {
        int exemplarRecallNumber = Integer.valueOf(parserConfig.getParameterValue(PARSER_EXEMPLAR_RECALL_NUMBER));
        int fewShotNumber = Integer.valueOf(parserConfig.getParameterValue(PARSER_FEW_SHOT_NUMBER));
        int selfConsistencyNumber = Integer.valueOf(parserConfig.getParameterValue(PARSER_SELF_CONSISTENCY_NUMBER));

        List<SqlExemplar> exemplars = Lists.newArrayList();
        llmReq.getExemplars().stream().forEach(e -> {
            exemplars.add(e);
        });

        int recallSize = exemplarRecallNumber - llmReq.getExemplars().size();
        if (recallSize > 0) {
            exemplars.addAll(exemplarService.recallExemplars(llmReq.getQueryText(), recallSize));
        }

        List<List<SqlExemplar>> results = new ArrayList<>();
        // use random collection of exemplars for each self-consistency inference
        for (int i = 0; i < selfConsistencyNumber; i++) {
            List<SqlExemplar> shuffledList = new ArrayList<>(exemplars);
            Collections.shuffle(shuffledList);
            results.add(shuffledList.subList(0, fewShotNumber));
        }

        return results;
    }

    public String buildAugmentedQuestion(LLMReq llmReq) {
        List<LLMReq.ElementValue> linkedValues = llmReq.getLinking();
        String currentDate = llmReq.getCurrentDate();
        String priorExts = llmReq.getPriorExts();

        List<String> priorLinkingList = new ArrayList<>();
        for (LLMReq.ElementValue value : linkedValues) {
            String fieldName = value.getFieldName();
            String fieldValue = value.getFieldValue();
            priorLinkingList.add("‘" + fieldValue + "‘是一个‘" + fieldName + "‘");
        }
        String currentDataStr = "当前的日期是" + currentDate;
        String linkingListStr = String.join("，", priorLinkingList);
        String termStr = buildTermStr(llmReq);
        return String.format("%s (补充信息:%s;%s;%s;%s)", llmReq.getQueryText(),
                linkingListStr, currentDataStr, termStr, priorExts);
    }

    public String buildSchemaStr(LLMReq llmReq) {
        String tableStr = llmReq.getSchema().getDataSetName();
        StringBuilder metricStr = new StringBuilder();
        StringBuilder dimensionStr = new StringBuilder();

        llmReq.getSchema().getMetrics().stream().forEach(
                metric -> {
                    metricStr.append(metric.getName());
                    if (StringUtils.isNotEmpty(metric.getDescription())) {
                        metricStr.append(" COMMENT '" + metric.getDescription() + "'");
                    }
                    if (StringUtils.isNotEmpty(metric.getDefaultAgg())) {
                        metricStr.append(" AGGREGATE '" + metric.getDefaultAgg().toUpperCase() + "'");
                    }
                    metricStr.append(",");
                }
        );

        llmReq.getSchema().getDimensions().stream().forEach(
                dimension -> {
                    dimensionStr.append(dimension.getName());
                    if (StringUtils.isNotEmpty(dimension.getDescription())) {
                        dimensionStr.append(" COMMENT '" + dimension.getDescription() + "'");
                    }
                    dimensionStr.append(",");
                }
        );

        String template = "Table: %s, Metrics: [%s], Dimensions: [%s]";


        return String.format(template, tableStr, metricStr, dimensionStr);
    }

    private String buildTermStr(LLMReq llmReq) {
        List<LLMReq.Term> terms = llmReq.getSchema().getTerms();
        StringBuilder termsDesc = new StringBuilder();
        if (!CollectionUtils.isEmpty(terms)) {
            termsDesc.append("相关业务术语：");
            for (int idx = 0; idx < terms.size(); idx++) {
                LLMReq.Term term = terms.get(idx);
                String name = term.getName();
                String description = term.getDescription();
                List<String> alias = term.getAlias();
                String descPart = StringUtils.isBlank(description) ? "" : String.format("，它通常是指<%s>", description);
                String aliasPart = CollectionUtils.isEmpty(alias) ? "" : String.format("，类似的表达还有%s", alias);
                termsDesc.append(String.format("%d.<%s>是业务术语%s%s；", idx + 1, name, descPart, aliasPart));
            }
            if (termsDesc.length() > 0) {
                termsDesc.setLength(termsDesc.length() - 1);
            }
        }

        return termsDesc.toString();
    }

}
