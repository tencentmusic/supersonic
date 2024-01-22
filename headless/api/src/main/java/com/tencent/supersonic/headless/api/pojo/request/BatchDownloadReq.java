package com.tencent.supersonic.headless.api.pojo.request;

import com.tencent.supersonic.common.pojo.DateConf;
import lombok.Data;
import java.util.List;

@Data
public class BatchDownloadReq {

    private List<Long> metricIds;

    private DateConf dateInfo;

    private boolean isTransform = true;

}
