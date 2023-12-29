package com.tencent.supersonic.headless.common.query.pojo;

import java.util.List;
import lombok.Data;

@Data
public class Materialization {

    private String name;
    private String destination;
    private String destinationType;
    private List<String> depends;
    private List<String> metrics;
    private List<String> dimensions;

}
