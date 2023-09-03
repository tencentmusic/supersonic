package com.tencent.supersonic.chat.api.pojo;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class SchemaValueMap {

    /**
     * dimension value in db
     */
    private String techName;

    /**
     * dimension value for result show
     */
    private String bizName;

    /**
     * dimension value for user query
     */
    private List<String> alias = new ArrayList<>();
}