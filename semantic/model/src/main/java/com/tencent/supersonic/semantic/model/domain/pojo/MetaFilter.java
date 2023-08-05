package com.tencent.supersonic.semantic.model.domain.pojo;

import lombok.Data;
import java.util.List;


@Data
public class MetaFilter {

    private Long id;

    private String name;

    private String bizName;

    private String createdBy;

    private List<Long> domainIds;

    private Integer sensitiveLevel;

    private Integer status;

}
