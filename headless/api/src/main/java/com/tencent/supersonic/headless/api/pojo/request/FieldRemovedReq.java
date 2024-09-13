package com.tencent.supersonic.headless.api.pojo.request;

import lombok.Data;

import java.util.List;

@Data
public class FieldRemovedReq {

    private Long modelId;

    private List<String> fields;
}
