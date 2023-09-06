package com.tencent.supersonic.semantic.query.persistence.repository;

import static com.tencent.supersonic.common.pojo.Constants.AT_SYMBOL;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.supersonic.semantic.api.model.pojo.QueryStat;
import com.tencent.supersonic.semantic.api.query.request.ItemUseReq;
import com.tencent.supersonic.semantic.api.query.response.ItemUseResp;
import com.tencent.supersonic.common.pojo.enums.TypeEnums;
import com.tencent.supersonic.semantic.query.persistence.mapper.StatMapper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
public class StatRepositoryImpl implements StatRepository {

    private final StatMapper statMapper;
    private final ObjectMapper mapper = new ObjectMapper();

    public StatRepositoryImpl(StatMapper statMapper) {
        this.statMapper = statMapper;
    }

    @Override
    public Boolean createRecord(QueryStat queryStatInfo) {
        return statMapper.createRecord(queryStatInfo);
    }

    @Override
    public List<ItemUseResp> getStatInfo(ItemUseReq itemUseCommend) {
        List<ItemUseResp> result = new ArrayList<>();
        List<QueryStat> statInfos = statMapper.getStatInfo(itemUseCommend);
        Map<String, Long> map = new ConcurrentHashMap<>();
        statInfos.stream().forEach(stat -> {
            String dimensions = stat.getDimensions();
            String metrics = stat.getMetrics();
            updateStatMapInfo(map, dimensions, TypeEnums.DIMENSION.getName(), stat.getModelId());
            updateStatMapInfo(map, metrics, TypeEnums.METRIC.getName(), stat.getModelId());
        });

        map.forEach((k, v) -> {
            Long classId = Long.parseLong(k.split(AT_SYMBOL + AT_SYMBOL)[0]);
            String type = k.split(AT_SYMBOL + AT_SYMBOL)[1];
            String nameEn = k.split(AT_SYMBOL + AT_SYMBOL)[2];
            result.add(new ItemUseResp(classId, type, nameEn, v));
        });

        List<ItemUseResp> itemUseResps = result.stream().sorted(Comparator.comparing(ItemUseResp::getUseCnt).reversed())
                .collect(Collectors.toList());
        return itemUseResps;
    }

    @Override
    public List<QueryStat> getQueryStatInfoWithoutCache(ItemUseReq itemUseCommend) {
        return statMapper.getStatInfo(itemUseCommend);
    }

    private void updateStatMapInfo(Map<String, Long> map, String dimensions, String type, Long modelId) {
        if (Strings.isNotEmpty(dimensions)) {
            try {
                List<String> dimensionList = mapper.readValue(dimensions, new TypeReference<List<String>>() {
                });
                dimensionList.stream().forEach(dimension -> {
                    String key = modelId + AT_SYMBOL + AT_SYMBOL + type + AT_SYMBOL + AT_SYMBOL + dimension;
                    if (map.containsKey(key)) {
                        map.put(key, map.get(key) + 1);
                    } else {
                        map.put(key, 1L);
                    }
                });
            } catch (Exception e) {
                log.warn("e:{}", e);
            }
        }
    }

    private void updateStatMapInfo(Map<String, Long> map, Long modelId, String type) {
        if (Objects.nonNull(modelId)) {
            String key = type + AT_SYMBOL + AT_SYMBOL + modelId;
            if (map.containsKey(key)) {
                map.put(key, map.get(key) + 1);
            } else {
                map.put(key, 1L);
            }
        }
    }
}