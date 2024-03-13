package com.tencent.supersonic.chat.api.pojo.response;


import com.tencent.supersonic.headless.api.pojo.response.QueryResp;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ShowCaseResp {

    private Map<Long, List<QueryResp>> showCaseMap;

    private int pageSize;

    private int current;

}
