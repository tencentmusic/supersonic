package com.tencent.supersonic.semantic.query.domain.parser;

import com.tencent.supersonic.semantic.query.domain.parser.dsl.SemanticModel;

public interface SemanticSchemaManager {

    // get the schema from cache   , if not exit , will refresh by the reload function
    SemanticModel get(String rootPath) throws Exception;

    // refresh cache ,will return the data get from db or other storage
    SemanticModel reload(String rootPath);
}
