package com.tencent.supersonic.headless.api.pojo;

import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DataSetModelConfig {

    private Long id;

    private Boolean includesAll = false;

    private List<Long> metrics = Lists.newArrayList();

    private List<Long> dimensions = Lists.newArrayList();

    public DataSetModelConfig(Long id, List<Long> dimensions, List<Long> metrics) {
        this.id = id;
        this.metrics = metrics;
        this.dimensions = dimensions;
    }
}
