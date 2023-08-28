package com.tencent.supersonic.semantic.model.domain.pojo;

import java.util.List;
import lombok.Data;


@Data
public class MetaFilter {

    private Long id;

    private String name;

    private String bizName;

    private String createdBy;

    private List<Long> modelIds;

    private Integer sensitiveLevel;

    private Integer status;

}
