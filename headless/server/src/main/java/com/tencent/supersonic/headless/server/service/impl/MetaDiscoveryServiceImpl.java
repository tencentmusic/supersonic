package com.tencent.supersonic.headless.server.service.impl;

import com.tencent.supersonic.common.pojo.enums.TimeDimensionEnum;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.headless.api.pojo.SchemaElementType;
import com.tencent.supersonic.headless.api.pojo.SchemaItem;
import com.tencent.supersonic.headless.api.pojo.SchemaMapInfo;
import com.tencent.supersonic.headless.api.pojo.SemanticSchema;
import com.tencent.supersonic.headless.api.pojo.Term;
import com.tencent.supersonic.headless.api.pojo.request.QueryMapReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryReq;
import com.tencent.supersonic.headless.api.pojo.response.DataSetMapInfo;
import com.tencent.supersonic.headless.api.pojo.response.DataSetResp;
import com.tencent.supersonic.headless.api.pojo.response.MapInfoResp;
import com.tencent.supersonic.headless.api.pojo.response.MapResp;
import com.tencent.supersonic.headless.core.chat.knowledge.builder.BaseWordBuilder;
import com.tencent.supersonic.headless.server.pojo.MetaFilter;
import com.tencent.supersonic.headless.server.service.ChatQueryService;
import com.tencent.supersonic.headless.server.service.DataSetService;
import com.tencent.supersonic.headless.server.service.MetaDiscoveryService;
import com.tencent.supersonic.headless.server.service.TermService;
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

    @Autowired
    private TermService termService;

    @Override
    public MapInfoResp getMapMeta(QueryMapReq queryMapReq) {

        QueryReq queryReq = new QueryReq();
        BeanUtils.copyProperties(queryMapReq, queryReq);
        List<DataSetResp> dataSets = dataSetService.getDataSets(queryMapReq.getDataSetNames(), queryMapReq.getUser());

        Set<Long> dataSetIds = dataSets.stream().map(SchemaItem::getId).collect(Collectors.toSet());
        queryReq.setDataSetIds(dataSetIds);
        MapResp mapResp = chatQueryService.performMapping(queryReq);
        return convert(mapResp, queryMapReq.getTopN());
    }

    private MapInfoResp convert(MapResp mapResp, Integer topN) {
        MapInfoResp mapInfoResp = new MapInfoResp();
        if (Objects.isNull(mapResp)) {
            return mapInfoResp;
        }
        BeanUtils.copyProperties(mapResp, mapInfoResp);
        Set<Long> dataSetIds = mapResp.getMapInfo().getDataSetElementMatches().keySet();
        MetaFilter metaFilter = new MetaFilter();
        metaFilter.setIds(new ArrayList<>(dataSetIds));
        List<DataSetResp> dataSetList = dataSetService.getDataSetList(metaFilter);
        Map<Long, DataSetResp> dataSetMap = dataSetList.stream()
                .collect(Collectors.toMap(DataSetResp::getId, d -> d));
        mapInfoResp.setDataSetMapInfo(getDataSetInfo(mapResp.getMapInfo(), dataSetMap, topN));
        mapInfoResp.setTerms(getTerms(dataSetList, dataSetMap));
        return mapInfoResp;
    }

    private Map<String, DataSetMapInfo> getDataSetInfo(SchemaMapInfo mapInfo,
                                                       Map<Long, DataSetResp> dataSetMap,
                                                       Integer topN) {
        Map<String, DataSetMapInfo> map = new HashMap<>();
        Map<Long, List<SchemaElementMatch>> mapFields = getMapFields(mapInfo, dataSetMap);
        Map<Long, List<SchemaElementMatch>> topFields = getTopFields(topN, mapInfo, dataSetMap);
        for (Long dataSetId : mapInfo.getDataSetElementMatches().keySet()) {
            DataSetResp dataSetResp = dataSetMap.get(dataSetId);
            if (dataSetResp == null) {
                continue;
            }
            DataSetMapInfo dataSetMapInfo = new DataSetMapInfo();
            dataSetMapInfo.setMapFields(mapFields.get(dataSetId));
            dataSetMapInfo.setTopFields(topFields.get(dataSetId));
            dataSetMapInfo.setName(dataSetResp.getName());
            dataSetMapInfo.setDescription(dataSetResp.getDescription());
            map.put(dataSetMapInfo.getName(), dataSetMapInfo);
        }
        return map;
    }

    private Map<Long, List<SchemaElementMatch>> getMapFields(SchemaMapInfo mapInfo,
                                                               Map<Long, DataSetResp> dataSetMap) {
        Map<Long, List<SchemaElementMatch>> result = new HashMap<>();
        for (Map.Entry<Long, List<SchemaElementMatch>> entry : mapInfo.getDataSetElementMatches().entrySet()) {
            List<SchemaElementMatch> values = entry.getValue();
            if (CollectionUtils.isNotEmpty(values) && dataSetMap.containsKey(entry.getKey())) {
                result.put(entry.getKey(), values);
            }
        }
        return result;
    }

    private Map<Long, List<SchemaElementMatch>> getTopFields(Integer topN,
                                                               SchemaMapInfo mapInfo,
                                                               Map<Long, DataSetResp> dataSetMap) {
        Map<Long, List<SchemaElementMatch>> result = new HashMap<>();

        SemanticSchema semanticSchema = semanticService.getSemanticSchema();
        for (Map.Entry<Long, List<SchemaElementMatch>> entry : mapInfo.getDataSetElementMatches().entrySet()) {
            Long dataSetId = entry.getKey();
            List<SchemaElementMatch> values = entry.getValue();
            String dataSetName = dataSetMap.get(dataSetId).getName();
            if (StringUtils.isBlank(dataSetName) || CollectionUtils.isEmpty(values)) {
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
            result.put(dataSetId, new ArrayList<>(dimensions));
        }
        return result;
    }

    private Map<String, List<Term>> getTerms(List<DataSetResp> dataSets, Map<Long, DataSetResp> dataSetNameMap) {
        Set<Long> domainIds = dataSets.stream().map(DataSetResp::getDomainId).collect(Collectors.toSet());
        Map<Long, Long> dataSetDomainIdMap = dataSets.stream()
                .collect(Collectors.toMap(DataSetResp::getId, DataSetResp::getDomainId));
        Map<Long, List<Term>> domainTermSetMap = termService.getTermSets(domainIds);
        Map<String, List<Term>> dataSetTermSetMap = new HashMap<>();
        for (DataSetResp dataSet : dataSets) {
            dataSetTermSetMap.put(dataSetNameMap.get(dataSet.getId()).getName(),
                    domainTermSetMap.get(dataSetDomainIdMap.get(dataSet.getId())));
        }
        return dataSetTermSetMap;
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
