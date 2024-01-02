package com.tencent.supersonic.headless.api.request;

import lombok.Data;

@Data
public class DownloadStructReq extends QueryStructReq {

    private boolean isTransform;

    public void setIsTransform(boolean isTransform) {
        this.isTransform = isTransform;
    }

}
