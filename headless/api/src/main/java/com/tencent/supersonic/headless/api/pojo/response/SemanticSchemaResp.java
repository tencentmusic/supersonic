package com.tencent.supersonic.headless.api.pojo.response;

import com.google.common.collect.Lists;
import com.tencent.supersonic.common.pojo.ModelRela;
import com.tencent.supersonic.headless.api.pojo.enums.SchemaType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

import static com.tencent.supersonic.common.pojo.Constants.UNDERLINE;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SemanticSchemaResp {

    private Long viewId;
    private List<Long> modelIds;
    private SchemaType schemaType;
    private List<MetricSchemaResp> metrics = Lists.newArrayList();
    private List<DimSchemaResp> dimensions = Lists.newArrayList();
    private List<ModelRela> modelRelas = Lists.newArrayList();
    private List<ModelResp> modelResps = Lists.newArrayList();
    private ViewResp viewResp;
    private DatabaseResp databaseResp;

    public String getSchemaKey() {
        if (viewId == null) {
            return String.format("%s_%s", schemaType, StringUtils.join(modelIds, UNDERLINE));
        }
        return String.format("%s_%s", schemaType, viewId);

    }

}