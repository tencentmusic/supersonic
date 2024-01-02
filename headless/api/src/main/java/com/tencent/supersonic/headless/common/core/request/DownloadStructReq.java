package com.tencent.supersonic.headless.common.core.request;

import lombok.Data;

@Data
public class DownloadStructReq extends QueryStructReq {

    private boolean isTransform;

    public void setIsTransform(boolean isTransform) {
        this.isTransform = isTransform;
    }

}
