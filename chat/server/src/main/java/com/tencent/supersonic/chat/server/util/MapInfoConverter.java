package com.tencent.supersonic.chat.server.util;

import com.tencent.supersonic.common.pojo.enums.TimeDimensionEnum;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.headless.api.pojo.SchemaElementType;
import com.tencent.supersonic.headless.api.pojo.SchemaMapInfo;
import com.tencent.supersonic.headless.api.pojo.SemanticSchema;
import com.tencent.supersonic.headless.api.pojo.response.MapInfoResp;
import com.tencent.supersonic.headless.api.pojo.response.MapResp;
import com.tencent.supersonic.headless.core.chat.knowledge.builder.BaseWordBuilder;
import com.tencent.supersonic.headless.server.service.impl.SemanticService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MapInfoConverter {

    public static MapInfoResp convert(MapResp mapResp, Integer topN) {
        MapInfoResp mapInfoResp = new MapInfoResp();
        if (Objects.isNull(mapResp)) {
            return mapInfoResp;
        }
        BeanUtils.copyProperties(mapResp, mapInfoResp);
        Map<Long, String> dataSetMap = mapResp.getMapInfo().generateDataSetMap();
        mapInfoResp.setMapFields(getMapFields(mapResp.getMapInfo(), dataSetMap));
        mapInfoResp.setTopFields(getTopFields(topN, mapResp.getMapInfo(), dataSetMap));
        return mapInfoResp;
    }

    private static Map<String, List<SchemaElementMatch>> getMapFields(SchemaMapInfo mapInfo,
                                                                      Map<Long, String> dataSetMap) {
        Map<String, List<SchemaElementMatch>> result = new HashMap<>();
        for (Map.Entry<Long, List<SchemaElementMatch>> entry : mapInfo.getDataSetElementMatches().entrySet()) {
            List<SchemaElementMatch> values = entry.getValue();
            if (CollectionUtils.isNotEmpty(values) && dataSetMap.containsKey(entry.getKey())) {
                result.put(dataSetMap.get(entry.getKey()), values);
            }
        }
        return result;
    }

    private static Map<String, List<SchemaElementMatch>> getTopFields(Integer topN,
                                                                      SchemaMapInfo mapInfo,
                                                                      Map<Long, String> dataSetMap) {
        Set<Long> dataSetIds = mapInfo.getDataSetElementMatches().keySet();
        Map<String, List<SchemaElementMatch>> result = new HashMap<>();

        SemanticService semanticService = ContextUtils.getBean(SemanticService.class);
        SemanticSchema semanticSchema = semanticService.getSemanticSchema();
        for (Long dataSetId : dataSetIds) {
            String dataSetName = dataSetMap.get(dataSetId);

            //topN dimensions
            Set<SchemaElementMatch> dimensions = semanticSchema.getDimensions(dataSetId)
                    .stream().sorted(Comparator.comparing(SchemaElement::getUseCnt).reversed())
                    .limit(topN - 1).map(mergeFunction()).collect(Collectors.toSet());

            SchemaElementMatch timeDimensionMatch = getTimeDimension(dataSetId, dataSetName);
            dimensions.add(timeDimensionMatch);

            //topN metrics
            Set<SchemaElementMatch> metrics = semanticSchema.getMetrics(dataSetId)
                    .stream().sorted(Comparator.comparing(SchemaElement::getUseCnt).reversed())
                    .limit(topN).map(mergeFunction()).collect(Collectors.toSet());

            dimensions.addAll(metrics);
            result.put(dataSetName, new ArrayList<>(dimensions));
        }
        return result;
    }

    /***
     * get time dimension SchemaElementMatch
     * @param dataSetId
     * @param dataSetName
     * @return
     */
    private static SchemaElementMatch getTimeDimension(Long dataSetId, String dataSetName) {
        SchemaElement element = SchemaElement.builder().dataSet(dataSetId).dataSetName(dataSetName)
                .type(SchemaElementType.DIMENSION).bizName(TimeDimensionEnum.DAY.getName()).build();

        SchemaElementMatch timeDimensionMatch = SchemaElementMatch.builder().element(element)
                .detectWord(TimeDimensionEnum.DAY.getChName()).word(TimeDimensionEnum.DAY.getChName())
                .similarity(1L).frequency(BaseWordBuilder.DEFAULT_FREQUENCY).build();

        return timeDimensionMatch;
    }

    private static Function<SchemaElement, SchemaElementMatch> mergeFunction() {
        return schemaElement -> SchemaElementMatch.builder().element(schemaElement)
                .frequency(BaseWordBuilder.DEFAULT_FREQUENCY).word(schemaElement.getName()).similarity(1)
                .detectWord(schemaElement.getName()).build();
    }
}
