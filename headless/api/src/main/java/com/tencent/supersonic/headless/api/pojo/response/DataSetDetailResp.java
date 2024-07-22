package com.tencent.supersonic.headless.api.pojo.response;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class DataSetDetailResp extends DataSetResp {

    private List<MetricResp> metrics = new ArrayList<>();

    private List<DimensionResp> dimensions = new ArrayList<>();

}
