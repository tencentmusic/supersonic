package com.tencent.supersonic.chat.server.processor.parse;

import com.tencent.supersonic.chat.server.pojo.ParseContext;
import com.tencent.supersonic.headless.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.headless.api.pojo.SchemaElementType;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.chat.parser.llm.DataSetMatchResult;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * ParseInfoSortProcessor sorts candidate parse info based on certain algorithm. \
 **/
@Slf4j
public class ParseInfoSortProcessor implements ParseResultProcessor {

    @Override
    public void process(ParseContext parseContext) {
        List<SemanticParseInfo> selectedParses = parseContext.getResponse().getSelectedParses();

        selectedParses.sort((o1, o2) -> {
            DataSetMatchResult mr1 = getDataSetMatchResult(o1.getElementMatches());
            DataSetMatchResult mr2 = getDataSetMatchResult(o2.getElementMatches());

            double difference = mr1.getMaxDatesetSimilarity() - mr2.getMaxDatesetSimilarity();
            if (difference == 0) {
                difference = mr1.getMaxMetricSimilarity() - mr2.getMaxMetricSimilarity();
                if (difference == 0) {
                    difference = mr1.getTotalSimilarity() - mr2.getTotalSimilarity();
                }
                if (difference == 0) {
                    difference = mr1.getMaxMetricUseCnt() - mr2.getMaxMetricUseCnt();
                }
            }
            return difference >= 0 ? -1 : 1;
        });
        // re-assign parseId
        for (int i = 0; i < selectedParses.size(); i++) {
            SemanticParseInfo parseInfo = selectedParses.get(i);
            parseInfo.setId(i + 1);
        }
    }

    private DataSetMatchResult getDataSetMatchResult(List<SchemaElementMatch> elementMatches) {
        double maxMetricSimilarity = 0;
        double maxDatasetSimilarity = 0;
        double totalSimilarity = 0;
        long maxMetricUseCnt = 0L;
        for (SchemaElementMatch match : elementMatches) {
            if (SchemaElementType.DATASET.equals(match.getElement().getType())) {
                maxDatasetSimilarity = Math.max(maxDatasetSimilarity, match.getSimilarity());
            }
            if (SchemaElementType.METRIC.equals(match.getElement().getType())) {
                maxMetricSimilarity = Math.max(maxMetricSimilarity, match.getSimilarity());
                if (Objects.nonNull(match.getElement().getUseCnt())) {
                    maxMetricUseCnt = Math.max(maxMetricUseCnt, match.getElement().getUseCnt());
                }
            }
            totalSimilarity += match.getSimilarity();
        }
        return DataSetMatchResult.builder().maxMetricSimilarity(maxMetricSimilarity)
                .maxDatesetSimilarity(maxDatasetSimilarity).totalSimilarity(totalSimilarity)
                .build();
    }

}
