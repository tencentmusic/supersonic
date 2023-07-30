package com.tencent.supersonic.chat.api.component;

import com.tencent.supersonic.chat.api.pojo.QueryContext;

/**
 * This interface defines the contract for a schema mapper that identifies references to schema
 * elements in natural language queries.
 *
 * The schema mapper matches queries against the knowledge base which is constructed using the
 * schema of semantic models.
 */
public interface SchemaMapper {

    void map(QueryContext queryContext);
}
