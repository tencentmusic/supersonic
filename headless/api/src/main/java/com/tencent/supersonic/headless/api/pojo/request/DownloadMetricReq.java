package com.tencent.supersonic.headless.api.pojo.request;

import lombok.Data;

@Data
public class DownloadMetricReq extends QueryMetricReq {

    private boolean isTransform;

    public void setIsTransform(boolean isTransform) {
        this.isTransform = isTransform;
    }

}
