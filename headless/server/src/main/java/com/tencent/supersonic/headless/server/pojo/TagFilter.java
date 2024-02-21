package com.tencent.supersonic.headless.server.pojo;


import java.util.List;
import lombok.Data;

@Data
public class TagFilter extends MetaFilter {

    private String type;
    private List<Integer> statusList;

}
