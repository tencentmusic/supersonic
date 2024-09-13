package com.tencent.supersonic.chat.api.pojo.response;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ShowCaseResp {

    private Map<Long, List<QueryResp>> showCaseMap;

    private int pageSize;

    private int current;
}
