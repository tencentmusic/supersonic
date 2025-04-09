package com.tencent.supersonic.headless.server.persistence.dataobject;

import lombok.Data;

@Data
public class DimensionValueDO {

    private String id;

    private Long dimId;

    private Long modelId;

    private String dimName;

    private String dimBizName;

    private Integer status;

    private String type;

    private String alias;

    private String dimValue;

    private String nature;

    private Long frequency;

    public String getId() {
        return dimId + "_" + dimValue;
    }
}
