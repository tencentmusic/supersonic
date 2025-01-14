package com.tencent.supersonic.headless.core.translator;

import com.tencent.supersonic.headless.core.pojo.QueryStatement;

/**
 * A semantic translator converts semantic query into SQL statement that can be executed against
 * physical data models.
 */
public interface SemanticTranslator {

    void translate(QueryStatement queryStatement) throws Exception;
}
