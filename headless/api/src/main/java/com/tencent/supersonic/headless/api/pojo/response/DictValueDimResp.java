package com.tencent.supersonic.headless.api.pojo.response;

import lombok.Data;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: kanedai
 * @date: 2024/9/29
 */
@Data
@ToString
public class DictValueDimResp extends DictValueResp {
    /** dimension value */
    private String bizName;

    /** dimension value for user query */
    private List<String> alias = new ArrayList<>();
}
