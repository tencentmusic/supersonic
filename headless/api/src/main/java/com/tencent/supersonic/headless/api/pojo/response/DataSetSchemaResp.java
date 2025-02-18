package com.tencent.supersonic.headless.api.pojo.response;

import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DataSetSchemaResp extends DataSetResp {

    private String databaseType;
    private String databaseVersion;
    private List<MetricSchemaResp> metrics = Lists.newArrayList();
    private List<DimSchemaResp> dimensions = Lists.newArrayList();
    private List<ModelResp> modelResps = Lists.newArrayList();
    private List<TermResp> termResps = Lists.newArrayList();

}
