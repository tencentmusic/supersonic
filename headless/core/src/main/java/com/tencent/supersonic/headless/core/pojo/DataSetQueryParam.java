package com.tencent.supersonic.headless.core.pojo;

import com.tencent.supersonic.headless.api.pojo.MetricTable;
import lombok.Data;

import java.util.List;

@Data
public class DataSetQueryParam {
    private String sql = "";
    private List<MetricTable> tables;
    private boolean supportWith = true;
    private boolean withAlias = true;
}
