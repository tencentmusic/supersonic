package com.tencent.supersonic.chat.api.component;

import com.tencent.supersonic.chat.api.pojo.QueryContext;

/**
 * A schema mapper identifies references to schema elements(metrics/dimensions/entities/values)
 * in user queries. It matches the query text against the knowledge base.
 */
public interface SchemaMapper {

    void map(QueryContext queryContext);
}
