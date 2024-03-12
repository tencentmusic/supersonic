package com.tencent.supersonic.headless.core.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Data
@AllArgsConstructor
@ToString
@NoArgsConstructor
public class Dim4Dict {

    private Long dimId;
    private String bizName;
    private List<String> blackList;
    private List<String> whiteList;
    private List<String> ruleList;

}