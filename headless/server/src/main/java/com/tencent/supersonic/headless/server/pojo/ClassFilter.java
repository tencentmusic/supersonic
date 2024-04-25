package com.tencent.supersonic.headless.server.pojo;

import lombok.Data;

@Data
public class ClassFilter extends MetaFilter {

    private String type;
    private Long dataSetId;
    private Long classId;
}