package com.tencent.supersonic.headless.api.pojo;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@ToString
public class QueryResult<T> implements Serializable {

    private int pageNo = -1;
    private int pageSize = -1;
    private long totalCount = -1;
    private List<T> resultList = new ArrayList<T>();
}