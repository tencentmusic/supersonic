package com.tencent.supersonic.semantic.api.query.request;

import lombok.Data;

@Data
public class DownloadStructReq extends QueryStructReq {

    private boolean isTransform;

    public void setIsTransform(boolean isTransform) {
        this.isTransform = isTransform;
    }

}
