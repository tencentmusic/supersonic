package com.tencent.supersonic.chat.domain.pojo.search;

import java.util.List;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Setter
@Getter
public class SearchResponse {

    private List<SearchResult> searchResults;

    public SearchResponse(List<SearchResult> searchResults) {
        this.searchResults = searchResults;
    }
}
