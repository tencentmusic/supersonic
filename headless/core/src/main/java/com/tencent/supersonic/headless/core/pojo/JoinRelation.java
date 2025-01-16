package com.tencent.supersonic.headless.core.pojo;

import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.tuple.Triple;

import java.util.List;

@Data
@Builder
public class JoinRelation {
    private Long id;
    private String left;
    private String right;
    private String joinType;
    private List<Triple<String, String, String>> joinCondition;
}
