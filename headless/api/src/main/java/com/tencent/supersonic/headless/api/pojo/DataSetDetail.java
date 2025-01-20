package com.tencent.supersonic.headless.api.pojo;

import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
public class DataSetDetail {

    private List<DataSetModelConfig> dataSetModelConfigs = Collections.emptyList();

}
