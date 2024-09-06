package com.tencent.supersonic.headless.api.pojo.response;

import lombok.Data;

import java.util.List;

@Data
public class SearchResp {

    private List<SearchResult> searchResults;

    public SearchResp(List<SearchResult> searchResults) {
        this.searchResults = searchResults;
    }
}
