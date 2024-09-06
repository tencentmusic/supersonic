package dev.langchain4j.store.embedding;

import lombok.Data;

import java.util.List;

@Data
public class RetrieveQueryResult {

    private String query;

    private List<Retrieval> retrieval;
}
