package com.tencent.supersonic.chat.query.plugin.webservice;

import com.tencent.supersonic.chat.query.plugin.WebBase;
import lombok.Data;

@Data
public class WebServiceResponse {

    private WebBase webBase;

    private Object result;

}
