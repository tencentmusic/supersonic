package com.tencent.supersonic.common.util;


import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@Builder
@ToString
@Data
public class ThreadContext {

    private String traceId;

    private String userName;

    private String token;

    private Map<String, String> extendInfo;

}