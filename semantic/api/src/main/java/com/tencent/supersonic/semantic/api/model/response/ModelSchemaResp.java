package com.tencent.supersonic.semantic.api.model.response;

import com.tencent.supersonic.common.pojo.ModelRela;
import com.tencent.supersonic.semantic.api.model.pojo.Identify;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ModelSchemaResp extends ModelResp {

    private List<MetricSchemaResp> metrics;
    private List<DimSchemaResp> dimensions;
    private List<ModelRela> modelRelas;

    public DimSchemaResp getPrimaryKey() {
        Identify identify = getPrimaryIdentify();
        if (identify == null) {
            return null;
        }
        for (DimSchemaResp dimension : dimensions) {
            if (identify.getBizName().equals(dimension.getBizName())) {
                dimension.setEntityAlias(identify.getEntityNames());
                return dimension;
            }
        }
        return null;
    }

}