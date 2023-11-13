package com.tencent.supersonic.chat.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@Data
@PropertySource("classpath:optimization.properties")
public class OptimizationConfig {

    @Value("${one.detection.size}")
    private Integer oneDetectionSize;
    @Value("${one.detection.max.size}")
    private Integer oneDetectionMaxSize;

    @Value("${metric.dimension.min.threshold}")
    private Double metricDimensionMinThresholdConfig;

    @Value("${metric.dimension.threshold}")
    private Double metricDimensionThresholdConfig;

    @Value("${dimension.value.threshold}")
    private Double dimensionValueThresholdConfig;

    @Value("${function.bonus.threshold}")
    private Double functionBonusThreshold;

    @Value("${long.text.threshold}")
    private Double longTextThreshold;

    @Value("${short.text.threshold}")
    private Double shortTextThreshold;

    @Value("${query.text.length.threshold}")
    private Integer queryTextLengthThreshold;

    @Value("${candidate.threshold}")
    private Double candidateThreshold;

    @Value("${user.s2SQL.switch:false}")
    private boolean useS2SqlSwitch;

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
}
