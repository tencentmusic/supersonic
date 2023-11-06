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

    @Value("${user.s2ql.switch:false}")
    private boolean useS2qlSwitch;

    @Value("${embedding.mapper.word.min:2}")
    private int embeddingMapperWordMin;

    @Value("${embedding.mapper.number:10}")
    private int embeddingMapperNumber;

    @Value("${embedding.mapper.round.number:10}")
    private int embeddingMapperRoundNumber;

    @Value("${embedding.mapper.sum.number:10}")
    private int embeddingMapperSumNumber;

    @Value("${embedding.mapper.distance.threshold:0.3}")
    private Double embeddingMapperDistanceThreshold;
}
