package com.tencent.supersonic.semantic.api.model.pojo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class QueryResult<T> implements Serializable {

    private int pageNo = -1;
    private int pageSize = -1;
    private long totalCount = -1;
    private List<T> resultList = new ArrayList<T>();
}