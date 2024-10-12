package com.tencent.supersonic.headless.api.pojo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DimValueMap {

    /** dimension value in db */
    private String techName;

    /** dimension value for result show */
    private String bizName;

    /** dimension value for user query */
    private List<String> alias = new ArrayList<>();

    private String value;
}
