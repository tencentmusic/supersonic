package com.tencent.supersonic.chat.api.pojo.request;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ChatDefaultConfigReq {

    private List<Long> dimensionIds = new ArrayList<>();
    private List<Long> metricIds = new ArrayList<>();
}
