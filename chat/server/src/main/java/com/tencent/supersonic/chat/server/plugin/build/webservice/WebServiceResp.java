package com.tencent.supersonic.chat.server.plugin.build.webservice;

import com.tencent.supersonic.chat.server.plugin.build.WebBase;
import lombok.Data;

@Data
public class WebServiceResp {

    private WebBase webBase;

    private Object result;
}
