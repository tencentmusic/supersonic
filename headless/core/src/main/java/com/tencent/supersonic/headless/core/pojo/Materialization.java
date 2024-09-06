package com.tencent.supersonic.headless.core.pojo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class Materialization {

    private String name;
    private Long id;
    private Long dataSetId;
    private List<String> columns;
    private List<String> partitions;
    private boolean isPartitioned;
    private String partitionName;
}
