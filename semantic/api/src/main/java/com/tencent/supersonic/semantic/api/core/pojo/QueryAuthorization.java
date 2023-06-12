package com.tencent.supersonic.semantic.api.core.pojo;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@Data
@ToString
@NoArgsConstructor
public class QueryAuthorization {

    private String domainName;
    private List<String> dimensionFilters;
    private List<String> dimensionFiltersDesc;
    private String message;
}