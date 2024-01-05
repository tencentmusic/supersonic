package com.tencent.supersonic.chat.core.query.plugin.webservice;

import com.tencent.supersonic.chat.core.query.plugin.WebBase;
import lombok.Data;


@Data
public class WebServiceResp {

    private WebBase webBase;

    private Object result;

}
