package com.tencent.supersonic.semantic.model.domain.pojo;

import lombok.Data;

import java.util.List;

@Data
public class ModelFilter extends MetaFilter {

    private Long databaseId;

    private List<Long> domainIds;

}
