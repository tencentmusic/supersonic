package dev.langchain4j.store.embedding.qdrant;

import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.comparison.IsEqualTo;
import dev.langchain4j.store.embedding.filter.comparison.IsIn;
import dev.langchain4j.store.embedding.filter.logical.And;
import dev.langchain4j.store.embedding.filter.logical.Or;
import io.qdrant.client.ConditionFactory;
import io.qdrant.client.grpc.Common.Condition;

import java.util.ArrayList;
import java.util.List;

class QdrantFilterConverter {

    private QdrantFilterConverter() {}

    static io.qdrant.client.grpc.Common.Filter convert(Filter filter) {
        List<Condition> must = new ArrayList<>();
        List<Condition> should = new ArrayList<>();
        if (filter instanceof And) {
            And and = (And) filter;
            must.add(ConditionFactory.filter(convert(and.left())));
            must.add(ConditionFactory.filter(convert(and.right())));
        } else if (filter instanceof Or) {
            Or or = (Or) filter;
            should.add(ConditionFactory.filter(convert(or.left())));
            should.add(ConditionFactory.filter(convert(or.right())));
        } else if (filter instanceof IsEqualTo) {
            IsEqualTo equalTo = (IsEqualTo) filter;
            must.add(ConditionFactory.matchKeyword(equalTo.key(),
                    equalTo.comparisonValue().toString()));
        } else if (filter instanceof IsIn) {
            IsIn in = (IsIn) filter;
            List<String> values = new ArrayList<>();
            for (Object value : in.comparisonValues()) {
                values.add(value.toString());
            }
            must.add(ConditionFactory.matchKeywords(in.key(), values));
        } else {
            throw new UnsupportedOperationException("Unsupported filter: " + filter);
        }
        return io.qdrant.client.grpc.Common.Filter.newBuilder().addAllMust(must)
                .addAllShould(should).build();
    }
}
