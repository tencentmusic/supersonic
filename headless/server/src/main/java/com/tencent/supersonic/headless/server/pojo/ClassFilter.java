package com.tencent.supersonic.headless.server.pojo;

import com.tencent.supersonic.headless.api.pojo.MetaFilter;
import lombok.Data;

@Data
public class ClassFilter extends MetaFilter {

    private String type;
    private Long dataSetId;
    private Long classId;
}
