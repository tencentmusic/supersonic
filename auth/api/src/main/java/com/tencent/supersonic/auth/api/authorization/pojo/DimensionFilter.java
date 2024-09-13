package com.tencent.supersonic.auth.api.authorization.pojo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DimensionFilter {

    private List<String> expressions = new ArrayList<>();
    private String description;
}
