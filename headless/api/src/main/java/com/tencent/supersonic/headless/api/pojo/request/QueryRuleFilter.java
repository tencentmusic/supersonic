package com.tencent.supersonic.headless.api.pojo.request;

import lombok.Data;

import java.util.List;

@Data
public class QueryRuleFilter {

    private List<Integer> statusList;

    private List<Long> ruleIds;

    private List<Long> dataSetIds;
}
