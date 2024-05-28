package com.tencent.supersonic.chat.server.processor.execute;

import com.tencent.supersonic.chat.server.pojo.ChatExecuteContext;
import com.tencent.supersonic.chat.server.util.ResultFormatter;
import com.tencent.supersonic.headless.api.pojo.response.QueryResult;

public class QueryResultFormatProcessor implements ExecuteResultProcessor {

    @Override
    public void process(ChatExecuteContext chatExecuteContext, QueryResult queryResult) {
        String textResult = ResultFormatter.transform2TextNew(queryResult.getQueryColumns(),
                queryResult.getQueryResults());
        queryResult.setTextResult(textResult);
    }

}
