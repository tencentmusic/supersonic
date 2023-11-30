package com.tencent.supersonic.chat.query.plugin.webservice;

import com.tencent.supersonic.chat.query.plugin.WebBase;
import lombok.Data;


@Data
public class WebServiceResp {

    private WebBase webBase;

    private Object result;

}
