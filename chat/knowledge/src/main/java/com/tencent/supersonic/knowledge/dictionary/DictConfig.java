package com.tencent.supersonic.knowledge.dictionary;

import java.util.List;

import lombok.Data;


@Data
public class DictConfig {

    private Long domainId;

    private List<DimValueInfo> dimValueInfoList;
}