package com.tencent.supersonic.common.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@AllArgsConstructor
@Data
@ToString
@NoArgsConstructor
public class QueryAuthorization {

    private String domainName;
    private List<String> dimensionFilters;
    private List<String> dimensionFiltersDesc;
    private String message;

    public QueryAuthorization(String message) {
        this.message = message;
    }
}
