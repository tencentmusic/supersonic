package com.tencent.supersonic.headless.api.pojo.request;

import com.tencent.supersonic.headless.api.pojo.enums.TagDefineType;
import lombok.Data;

import java.util.List;

@Data
public class TagDeleteReq {

    private List<Long> ids;
    private List<Long> itemIds;
    private TagDefineType tagDefineType;
}
