package com.tencent.supersonic.headless.server.persistence.repository.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.supersonic.common.pojo.enums.TypeEnums;
import com.tencent.supersonic.headless.api.pojo.QueryStat;
import com.tencent.supersonic.headless.api.pojo.request.ItemUseReq;
import com.tencent.supersonic.headless.api.pojo.response.ItemUseResp;
import com.tencent.supersonic.headless.server.persistence.mapper.StatMapper;
import com.tencent.supersonic.headless.server.persistence.repository.StatRepository;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.tencent.supersonic.common.pojo.Constants.AT_SYMBOL;

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
    @SneakyThrows
    public List<ItemUseResp> getStatInfo(ItemUseReq itemUseReq) {
        List<ItemUseResp> result = new ArrayList<>();
        List<QueryStat> statInfos = statMapper.getStatInfo(itemUseReq);
        Map<String, Long> map = new ConcurrentHashMap<>();
        statInfos.stream().forEach(stat -> {
            String dimensions = stat.getDimensions();
            String metrics = stat.getMetrics();
            if (Objects.nonNull(stat.getDataSetId())) {
                updateStatMapInfo(map, dimensions, TypeEnums.DIMENSION.name().toLowerCase(), stat.getDataSetId());
                updateStatMapInfo(map, metrics, TypeEnums.METRIC.name().toLowerCase(), stat.getDataSetId());
            }
        });
        map.forEach((k, v) -> {
            Long classId = Long.parseLong(k.split(AT_SYMBOL + AT_SYMBOL)[0]);
            String type = k.split(AT_SYMBOL + AT_SYMBOL)[1];
            String nameEn = k.split(AT_SYMBOL + AT_SYMBOL)[2];
            result.add(new ItemUseResp(classId, type, nameEn, v));
        });

        return result.stream().sorted(Comparator.comparing(ItemUseResp::getUseCnt).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public List<QueryStat> getQueryStatInfoWithoutCache(ItemUseReq itemUseCommend) {
        return statMapper.getStatInfo(itemUseCommend);
    }

    private void updateStatMapInfo(Map<String, Long> map, String dimensions, String type, Long dataSetId) {
        if (StringUtils.isNotEmpty(dimensions)) {
            try {
                List<String> dimensionList = mapper.readValue(dimensions, new TypeReference<List<String>>() {
                });
                dimensionList.stream().forEach(dimension -> {
                    String key = dataSetId + AT_SYMBOL + AT_SYMBOL + type + AT_SYMBOL + AT_SYMBOL + dimension;
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
