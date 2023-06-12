package com.tencent.supersonic.chat.api.service;

import com.tencent.supersonic.chat.api.request.QueryContextReq;

/**
 * This interface defines the contract for a schema mapper that identifies references to schema
 * elements in natural language queries.
 *
 * The schema mapper matches queries against the knowledge base which is constructed using the
 * schema of semantic models.
 */
public interface SchemaMapper {

    void map(QueryContextReq searchCtx);
}
