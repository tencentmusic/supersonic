package com.tencent.supersonic.semantic.model.domain.pojo;

import com.tencent.supersonic.common.pojo.DataFormat;
import com.tencent.supersonic.semantic.api.model.pojo.MetricTypeParams;
import com.tencent.supersonic.semantic.api.model.pojo.RelateDimension;
import com.tencent.supersonic.semantic.api.model.pojo.SchemaItem;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import java.util.List;

@Data
public class Metric extends SchemaItem {


    private Long modelId;

    //measure_proxy ratio expr cumulative derived
    private String type;

    private MetricTypeParams typeParams;

    private String dataFormatType;

    private DataFormat dataFormat;

    private String alias;

    private List<String> tags;

    private RelateDimension relateDimension;

    public String getTag() {
        if (CollectionUtils.isEmpty(tags)) {
            return "";
        }
        return StringUtils.join(tags, ",");
    }

}
