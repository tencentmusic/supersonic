package com.tencent.supersonic.chat.postprocessor;
import com.tencent.supersonic.chat.api.pojo.QueryContext;

/**
 * A post processor do some logic after parser and corrector
 */

public interface PostProcessor {

    void process(QueryContext queryContext);

}
