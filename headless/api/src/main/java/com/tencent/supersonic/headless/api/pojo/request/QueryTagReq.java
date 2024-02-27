package com.tencent.supersonic.headless.api.pojo.request;

import java.util.List;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class QueryTagReq {

    private Long domainId;

    private List<Long> tagIds;

    private List<String> tagNames;

    private Long limit = 2000L;
}