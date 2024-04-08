package com.tencent.supersonic.headless.server.service.impl;

import com.tencent.supersonic.common.pojo.enums.TimeDimensionEnum;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.headless.api.pojo.SchemaElementType;
import com.tencent.supersonic.headless.api.pojo.SchemaMapInfo;
import com.tencent.supersonic.headless.api.pojo.SemanticSchema;
import com.tencent.supersonic.headless.api.pojo.request.QueryMapReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryReq;
import com.tencent.supersonic.headless.api.pojo.response.DataSetResp;
import com.tencent.supersonic.headless.api.pojo.response.MapInfoResp;
import com.tencent.supersonic.headless.api.pojo.response.MapResp;
import com.tencent.supersonic.headless.core.chat.knowledge.builder.BaseWordBuilder;
import com.tencent.supersonic.headless.server.service.ChatQueryService;
import com.tencent.supersonic.headless.server.service.DataSetService;
import com.tencent.supersonic.headless.server.service.MetaDiscoveryService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MetaDiscoveryServiceImpl implements MetaDiscoveryService {

    @Autowired
    private DataSetService dataSetService;

    @Autowired
    private ChatQueryService chatQueryService;

    @Autowired
    private SemanticService semanticService;

    @Override
    public MapInfoResp getMapMeta(QueryMapReq queryMapReq) {

        QueryReq queryReq = new QueryReq();
        BeanUtils.copyProperties(queryMapReq, queryReq);
        List<DataSetResp> dataSets = dataSetService.getDataSets(queryMapReq.getDataSetNames(), queryMapReq.getUser());
        Set<Long> dataSetIds = dataSets.stream().map(dataSetResp -> dataSetResp.getId()).collect(Collectors.toSet());
        queryReq.setDataSetIds(dataSetIds);

        MapResp mapResp = chatQueryService.performMapping(queryReq);
        return convert(mapResp, queryMapReq.getTopN());
    }

    public MapInfoResp convert(MapResp mapResp, Integer topN) {
        MapInfoResp mapInfoResp = new MapInfoResp();
        if (Objects.isNull(mapResp)) {
            return mapInfoResp;
        }
        BeanUtils.copyProperties(mapResp, mapInfoResp);
        Set<Long> dataSetIds = mapResp.getMapInfo().getDataSetElementMatches().keySet();
        Map<Long, String> dataSetMap = dataSetService.getDataSetIdToNameMap(new ArrayList<>(dataSetIds));

        mapInfoResp.setMapFields(getMapFields(mapResp.getMapInfo(), dataSetMap));
        mapInfoResp.setTopFields(getTopFields(topN, mapResp.getMapInfo(), dataSetMap));
        return mapInfoResp;
    }

    private Map<String, List<SchemaElementMatch>> getMapFields(SchemaMapInfo mapInfo,
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

    private Map<String, List<SchemaElementMatch>> getTopFields(Integer topN,
                                                               SchemaMapInfo mapInfo,
                                                               Map<Long, String> dataSetMap) {
        Set<Long> dataSetIds = mapInfo.getDataSetElementMatches().keySet();
        Map<String, List<SchemaElementMatch>> result = new HashMap<>();

        SemanticSchema semanticSchema = semanticService.getSemanticSchema();
        for (Long dataSetId : dataSetIds) {
            String dataSetName = dataSetMap.get(dataSetId);
            if (StringUtils.isBlank(dataSetName)) {
                continue;
            }
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
    private SchemaElementMatch getTimeDimension(Long dataSetId, String dataSetName) {
        SchemaElement element = SchemaElement.builder().dataSet(dataSetId).dataSetName(dataSetName)
                .type(SchemaElementType.DIMENSION).bizName(TimeDimensionEnum.DAY.getName()).build();

        SchemaElementMatch timeDimensionMatch = SchemaElementMatch.builder().element(element)
                .detectWord(TimeDimensionEnum.DAY.getChName()).word(TimeDimensionEnum.DAY.getChName())
                .similarity(1L).frequency(BaseWordBuilder.DEFAULT_FREQUENCY).build();

        return timeDimensionMatch;
    }

    private Function<SchemaElement, SchemaElementMatch> mergeFunction() {
        return schemaElement -> SchemaElementMatch.builder().element(schemaElement)
                .frequency(BaseWordBuilder.DEFAULT_FREQUENCY).word(schemaElement.getName()).similarity(1)
                .detectWord(schemaElement.getName()).build();
    }
}
