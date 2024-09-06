package com.tencent.supersonic.headless.api.pojo.request;

import com.tencent.supersonic.common.pojo.DataFormat;
import com.tencent.supersonic.headless.api.pojo.RelateDimension;
import com.tencent.supersonic.headless.api.pojo.SchemaItem;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

@Data
public class MetricBaseReq extends SchemaItem {

    private Long modelId;

    private String alias;

    private String dataFormatType;

    private DataFormat dataFormat;

    private List<String> classifications;

    private RelateDimension relateDimension;

    private int isTag;

    private Map<String, Object> ext;

    public String getClassifications() {
        if (classifications == null) {
            return null;
        }
        if (CollectionUtils.isEmpty(classifications)) {
            return "";
        }
        return StringUtils.join(classifications, ",");
    }
}
