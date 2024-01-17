package com.tencent.supersonic.headless.api.request;

import lombok.Data;
import java.util.List;

@Data
public class FieldRemovedReq {

    private Long modelId;

    private List<String> fields;

}
