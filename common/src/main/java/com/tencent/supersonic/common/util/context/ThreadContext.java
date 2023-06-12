package com.tencent.supersonic.common.util.context;


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

}