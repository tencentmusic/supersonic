package com.tencent.supersonic.headless.server.pojo;


import java.util.List;

import com.tencent.supersonic.headless.api.pojo.enums.TagDefineType;
import lombok.Data;

@Data
public class TagFilter extends MetaFilter {

    private String type;
    private List<Integer> statusList;
    private TagDefineType tagDefineType;
    private List<String> bizNames;

}
