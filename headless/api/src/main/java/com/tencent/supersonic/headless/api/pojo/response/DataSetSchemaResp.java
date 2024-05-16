package com.tencent.supersonic.headless.api.pojo.response;

import com.google.common.collect.Lists;
import com.tencent.supersonic.headless.api.pojo.Identify;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DataSetSchemaResp extends DataSetResp {

    private List<MetricSchemaResp> metrics = Lists.newArrayList();
    private List<DimSchemaResp> dimensions = Lists.newArrayList();
    private List<ModelResp> modelResps = Lists.newArrayList();
    private List<TermResp> termResps = Lists.newArrayList();

    public DimSchemaResp getPrimaryKey() {
        for (ModelResp modelResp : modelResps) {
            Identify identify = modelResp.getPrimaryIdentify();
            if (identify == null) {
                continue;
            }
            for (DimSchemaResp dimension : dimensions) {
                if (identify.getBizName().equals(dimension.getBizName())) {
                    dimension.setEntityAlias(identify.getEntityNames());
                    return dimension;
                }
            }
        }
        return null;
    }
}