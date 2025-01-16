package com.tencent.supersonic.headless.chat.utils;

import com.tencent.supersonic.headless.api.pojo.DimensionMappingConfig;
import com.tencent.supersonic.headless.api.pojo.SchemaElementType;
import com.tencent.supersonic.headless.chat.ChatQueryContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class DimensionValuesMatchUtils {

    @Autowired
    private DimensionMappingConfig dimensionMappingConfig;

    public void processDimensions(Map<SchemaElementType, List<String>> elementTypeToNatureMap,
            ChatQueryContext chatQueryContext) {
        Set<Long> dataSetIds = chatQueryContext.getRequest().getDataSetIds();
        if (!dataSetIds.contains(dimensionMappingConfig.getDataSet())) {
            return;
        }

        // 从配置中提取维度 ID
        Set<String> dimensionIdsFromElementMap = extractDimensionIds(elementTypeToNatureMap);


        if (Boolean.TRUE.equals(getJudgeByType(dimensionIdsFromElementMap))) {
            return;
        }

        List<Map.Entry<String, String>> dimensionValusAndIdMap =
                generateDimensionValusAndIdMap(elementTypeToNatureMap);
        storeToChatQueryContext(chatQueryContext, dimensionValusAndIdMap);
    }

    private Set<String> extractDimensionIds(
            Map<SchemaElementType, List<String>> elementTypeToNatureMap) {
        return elementTypeToNatureMap.values().stream().flatMap(List::stream).map(value -> {
            String[] parts = value.split("_");
            return parts.length == 3 ? parts[2] : null;
        }).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    private void storeToChatQueryContext(ChatQueryContext chatQueryContext,
            List<Map.Entry<String, String>> dimensionValusAndIdMap) {
        chatQueryContext.setIsTip(true);
        chatQueryContext.setSchemaValusByTerm(dimensionValusAndIdMap);
    }

    private Boolean getJudgeByType(Set<String> dimensionIds) {
        // 获取维度ID配置
        String dimPid1 = dimensionMappingConfig.getMapping().get("pid1_2022");
        String dimPid2 = dimensionMappingConfig.getMapping().get("pid2_2022");
        String dimPid3 = dimensionMappingConfig.getMapping().get("pid3_2022");
        String dimPid4 = dimensionMappingConfig.getMapping().get("pid4_2022");
        boolean hasPid1 = dimensionIds.contains(dimPid1);
        boolean hasPid2 = dimensionIds.contains(dimPid2);
        boolean hasPid3 = dimensionIds.contains(dimPid3);
        boolean hasPid4 = dimensionIds.contains(dimPid4);

        // 条件判断，仅有1级时返回true
        if (dimensionIds.stream().filter(dimPid1::equals).count() == 1 && !hasPid2 && !hasPid3
                && !hasPid4) {
            return true;
        }

        // 条件2：当维度ID同时存在123级时，返回true
        if (hasPid1 && hasPid2 && hasPid3) {
            return true;
        }

        // 条件3：当维度ID 1234级 都不存在时，返回true
        if (!hasPid1 && !hasPid2 && !hasPid3 && !hasPid4) {
            return true;
        }
        return false;
    }

    private List<Map.Entry<String, String>> generateDimensionValusAndIdMap(
            Map<SchemaElementType, List<String>> elementTypeToNatureMap) {
        return elementTypeToNatureMap.values().stream().flatMap(List::stream)
                .map(value -> value.split("_")).filter(parts -> parts.length >= 3)
                .map(parts -> new AbstractMap.SimpleEntry<>(parts[2], parts[0]))
                .collect(Collectors.toList());
    }

}
