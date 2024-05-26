package com.tencent.supersonic.headless.core.config;

import com.tencent.supersonic.common.service.SysParameterService;
import com.tencent.supersonic.headless.core.chat.query.llm.s2sql.LLMReq;
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

    @Value("${s2.one.detection.size:8}")
    private Integer oneDetectionSize;
    @Value("${s2.one.detection.max.size:20}")
    private Integer oneDetectionMaxSize;

    @Value("${s2.one.detection.dimensionValue.size:1}")
    private Integer oneDetectionDimensionValueSize;

    @Value("${s2.metric.dimension.min.threshold:0.3}")
    private Double metricDimensionMinThresholdConfig;

    @Value("${s2.metric.dimension.threshold:0.3}")
    private Double metricDimensionThresholdConfig;

    @Value("${s2.dimension.value.min.threshold:0.2}")
    private Double dimensionValueMinThresholdConfig;

    @Value("${s2.dimension.value.threshold:0.5}")
    private Double dimensionValueThresholdConfig;

    @Value("${s2.long.text.threshold:0.8}")
    private Double longTextThreshold;

    @Value("${s2.short.text.threshold:0.5}")
    private Double shortTextThreshold;

    @Value("${s2.query.text.length.threshold:10}")
    private Integer queryTextLengthThreshold;

    @Value("${s2.embedding.mapper.word.min:4}")
    private int embeddingMapperWordMin;

    @Value("${s2.embedding.mapper.word.max:4}")
    private int embeddingMapperWordMax;

    @Value("${s2.embedding.mapper.batch:50}")
    private int embeddingMapperBatch;

    @Value("${s2.embedding.mapper.number:5}")
    private int embeddingMapperNumber;

    @Value("${s2.embedding.mapper.round.number:10}")
    private int embeddingMapperRoundNumber;

    @Value("${s2.embedding.mapper.min.threshold:0.6}")
    private Double embeddingMapperMinThreshold;

    @Value("${s2.embedding.mapper.threshold:0.99}")
    private Double embeddingMapperThreshold;

    @Value("${s2.parser.linking.value.switch:true}")
    private boolean useLinkingValueSwitch;

    @Value("${s2.parser.generation:TWO_PASS_AUTO_COT}")
    private LLMReq.SqlGenType sqlGenType;

    @Value("${s2.parser.use.switch:true}")
    private boolean useS2SqlSwitch;

    @Value("${s2.parser.exemplar-recall.num:15}")
    private int text2sqlExampleNum;

    @Value("${s2.parser.few-shot.num:10}")
    private int text2sqlFewShotsNum;

    @Value("${s2.parser.self-consistency.num:5}")
    private int text2sqlSelfConsistencyNum;

    @Value("${s2.parser.show-count:3}")
    private Integer parseShowCount;

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

    public Double getDimensionValueMinThresholdConfig() {
        return convertValue("dimension.value.min.threshold", Double.class, dimensionValueMinThresholdConfig);
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

    public Double getEmbeddingMapperMinThreshold() {
        return convertValue("embedding.mapper.min.threshold", Double.class, embeddingMapperMinThreshold);
    }

    public Double getEmbeddingMapperThreshold() {
        return convertValue("embedding.mapper.threshold", Double.class, embeddingMapperThreshold);
    }

    public boolean isUseLinkingValueSwitch() {
        return convertValue("s2SQL.linking.value.switch", Boolean.class, useLinkingValueSwitch);
    }

    public LLMReq.SqlGenType getSqlGenType() {
        return convertValue("s2SQL.generation", LLMReq.SqlGenType.class, sqlGenType);
    }

    public Integer getParseShowCount() {
        return convertValue("parse.show.count", Integer.class, parseShowCount);
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
            } else if (targetType == LLMReq.SqlGenType.class) {
                return targetType.cast(LLMReq.SqlGenType.valueOf(value));
            }
        } catch (Exception e) {
            log.error("convertValue", e);
        }
        return defaultValue;
    }

}
