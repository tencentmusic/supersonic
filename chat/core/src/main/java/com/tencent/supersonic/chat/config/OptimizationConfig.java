package com.tencent.supersonic.chat.config;

import com.tencent.supersonic.common.service.SysParameterService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
@Slf4j
public class OptimizationConfig {

    @Value("${one.detection.size:8}")
    private Integer oneDetectionSize;

    @Value("${one.detection.max.size:20}")
    private Integer oneDetectionMaxSize;

    @Value("${metric.dimension.min.threshold:0.3}")
    private Double metricDimensionMinThresholdConfig;

    @Value("${metric.dimension.threshold:0.3}")
    private Double metricDimensionThresholdConfig;

    @Value("${dimension.value.threshold:0.5}")
    private Double dimensionValueThresholdConfig;

    @Value("${long.text.threshold:0.8}")
    private Double longTextThreshold;

    @Value("${short.text.threshold:0.5}")
    private Double shortTextThreshold;

    @Value("${query.text.length.threshold:10}")
    private Integer queryTextLengthThreshold;
    @Value("${embedding.mapper.word.min:4}")
    private int embeddingMapperWordMin;

    @Value("${embedding.mapper.word.max:5}")
    private int embeddingMapperWordMax;

    @Value("${embedding.mapper.batch:50}")
    private int embeddingMapperBatch;

    @Value("${embedding.mapper.number:5}")
    private int embeddingMapperNumber;

    @Value("${embedding.mapper.round.number:10}")
    private int embeddingMapperRoundNumber;

    @Value("${embedding.mapper.distance.threshold:0.58}")
    private Double embeddingMapperDistanceThreshold;

    @Value("${s2SQL.linking.value.switch:true}")
    private boolean useLinkingValueSwitch;

    @Value("${s2SQL.use.switch:true}")
    private boolean useS2SqlSwitch;

    @Value("${text2sql.example.num:10}")
    private int text2sqlExampleNum;

    @Value("${text2sql.fewShots.num:10}")
    private int text2sqlFewShotsNum;

    @Value("${text2sql.self.consistency.num:5}")
    private int text2sqlSelfConsistencyNum;

    @Value("${text2sql.collection.name:text2dsl_agent_collection}")
    private String text2sqlCollectionName;

    @Autowired
    private SysParameterService sysParameterService;

    public Integer getOneDetectionSize() {
        return convertValue("one.detection.size", Integer.class, oneDetectionSize);
    }

    public Integer getOneDetectionMaxSize() {
        return convertValue("one.detection.max.size", Integer.class, oneDetectionMaxSize);
    }

    public Double getMetricDimensionMinThresholdConfig() {
        return convertValue("metric.dimension.min.threshold", Double.class, metricDimensionMinThresholdConfig);
    }

    public Double getMetricDimensionThresholdConfig() {
        return convertValue("metric.dimension.threshold", Double.class, metricDimensionThresholdConfig);
    }

    public Double getDimensionValueThresholdConfig() {
        return convertValue("dimension.value.threshold", Double.class, dimensionValueThresholdConfig);
    }

    public Double getLongTextThreshold() {
        return convertValue("long.text.threshold", Double.class, longTextThreshold);
    }

    public Double getShortTextThreshold() {
        return convertValue("short.text.threshold", Double.class, shortTextThreshold);
    }

    public Integer getQueryTextLengthThreshold() {
        return convertValue("query.text.length.threshold", Integer.class, queryTextLengthThreshold);
    }

    public boolean isUseS2SqlSwitch() {
        return convertValue("use.s2SQL.switch", Boolean.class, useS2SqlSwitch);
    }

    public Integer getEmbeddingMapperWordMin() {
        return convertValue("embedding.mapper.word.min", Integer.class, embeddingMapperWordMin);
    }

    public Integer getEmbeddingMapperWordMax() {
        return convertValue("embedding.mapper.word.max", Integer.class, embeddingMapperWordMax);
    }

    public Integer getEmbeddingMapperBatch() {
        return convertValue("embedding.mapper.batch", Integer.class, embeddingMapperBatch);
    }

    public Integer getEmbeddingMapperNumber() {
        return convertValue("embedding.mapper.number", Integer.class, embeddingMapperNumber);
    }

    public Integer getEmbeddingMapperRoundNumber() {
        return convertValue("embedding.mapper.round.number", Integer.class, embeddingMapperRoundNumber);
    }

    public Double getEmbeddingMapperDistanceThreshold() {
        return convertValue("embedding.mapper.distance.threshold", Double.class, embeddingMapperDistanceThreshold);
    }

    public boolean isUseLinkingValueSwitch() {
        return convertValue("s2SQL.linking.value.switch", Boolean.class, useLinkingValueSwitch);
    }

    public <T> T convertValue(String paramName, Class<T> targetType, T defaultValue) {
        try {
            String value = sysParameterService.getSysParameter().getParameterByName(paramName);
            if (StringUtils.isBlank(value)) {
                return defaultValue;
            }
            if (targetType == Double.class) {
                return targetType.cast(Double.parseDouble(value));
            } else if (targetType == Integer.class) {
                return targetType.cast(Integer.parseInt(value));
            } else if (targetType == Boolean.class) {
                return targetType.cast(Boolean.parseBoolean(value));
            }
        } catch (Exception e) {
            log.error("convertValue", e);
        }
        return defaultValue;
    }

}
