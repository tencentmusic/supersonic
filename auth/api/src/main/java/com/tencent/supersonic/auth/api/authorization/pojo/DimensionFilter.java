package com.tencent.supersonic.auth.api.authorization.pojo;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class DimensionFilter {

    private List<String> expressions = new ArrayList<>();
    private String description;
}
