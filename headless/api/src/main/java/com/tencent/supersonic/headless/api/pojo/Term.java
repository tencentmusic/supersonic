package com.tencent.supersonic.headless.api.pojo;

import com.google.common.collect.Lists;
import lombok.Data;
import java.util.List;

@Data
public class Term {

    private String name;

    private String description;

    private List<String> similarTerms = Lists.newArrayList();

}
