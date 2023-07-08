package com.tencent.supersonic.chat.api.pojo;

import lombok.Data;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class QueryFilter {

    private List<Filter> filters = new ArrayList<>();

    private Map<String, Object> params = new HashMap<>();

}
