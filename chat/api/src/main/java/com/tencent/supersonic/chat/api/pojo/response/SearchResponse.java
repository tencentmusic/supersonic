package com.tencent.supersonic.chat.api.pojo.response;

import java.util.List;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class SearchResponse {

    private List<SearchResult> searchResults;

    public SearchResponse(List<SearchResult> searchResults) {
        this.searchResults = searchResults;
    }
}
