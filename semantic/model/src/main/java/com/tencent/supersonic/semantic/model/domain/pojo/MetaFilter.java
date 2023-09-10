package com.tencent.supersonic.semantic.model.domain.pojo;

import lombok.Data;
import java.util.List;


@Data
public class MetaFilter {

    private String id;

    private String name;

    private String bizName;

    private String createdBy;

    private List<Long> modelIds;

    private Integer sensitiveLevel;

    private Integer status;

    private String key;

}
