package com.tencent.supersonic.common.util;

import com.tencent.supersonic.common.pojo.User;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.util.Map;

@Builder
@ToString
@Data
public class ThreadContext {

    private String traceId;

    private User user;

    private String token;

    private Map<String, String> extendInfo;
}
