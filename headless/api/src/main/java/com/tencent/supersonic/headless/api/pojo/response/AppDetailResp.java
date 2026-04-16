package com.tencent.supersonic.headless.api.pojo.response;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class AppDetailResp extends AppResp {

    private String appSecret;
}
