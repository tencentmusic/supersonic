package com.tencent.supersonic.headless.api.pojo.request;

import com.tencent.supersonic.common.pojo.RecordInfo;
import lombok.Data;

@Data
public class CanvasReq extends RecordInfo {

    private Long id;

    private Long domainId;

    private String type;

    private String config;
}
