package com.tencent.supersonic.headless.api.pojo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Field {

    private String fieldName;

    private String dataType;

}
