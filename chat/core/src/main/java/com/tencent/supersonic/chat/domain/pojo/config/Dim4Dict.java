package com.tencent.supersonic.chat.domain.pojo.config;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

@Data
@AllArgsConstructor
@ToString
public class Dim4Dict {

    private Long dimId;
    private String bizName;
    private List<String> blackList;
    private List<String> whiteList;
    private List<String> ruleList;

}