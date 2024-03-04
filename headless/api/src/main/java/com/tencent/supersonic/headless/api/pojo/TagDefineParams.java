package com.tencent.supersonic.headless.api.pojo;

import lombok.Data;

import java.util.List;

@Data
public class TagDefineParams {

    private String expr;
    private List<Object> dependencies;
}
