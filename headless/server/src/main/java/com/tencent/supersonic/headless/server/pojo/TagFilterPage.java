package com.tencent.supersonic.headless.server.pojo;


import com.tencent.supersonic.headless.api.pojo.enums.TagDefineType;
import com.tencent.supersonic.headless.api.pojo.request.PageSchemaItemReq;

import java.util.List;

public class TagFilterPage extends PageSchemaItemReq {
    private String type;
    private List<Integer> statusList;
    private TagDefineType tagDefineType;
}