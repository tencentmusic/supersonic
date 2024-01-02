package com.tencent.supersonic.headless.common.server.request;

import com.tencent.supersonic.common.pojo.DataFormat;
import com.tencent.supersonic.headless.common.server.pojo.RelateDimension;
import com.tencent.supersonic.headless.common.server.pojo.SchemaItem;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Data
public class MetricBaseReq extends SchemaItem {

    private Long modelId;

    private String alias;

    private String dataFormatType;

    private DataFormat dataFormat;

    private List<String> tags;

    private RelateDimension relateDimension;

    private Map<String, Object> ext = new HashMap<>();

    public String getTag() {
        if (CollectionUtils.isEmpty(tags)) {
            return "";
        }
        return StringUtils.join(tags, ",");
    }

}
