package com.tencent.supersonic.chat.integration.plugin;

import com.tencent.supersonic.BaseApplication;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.api.pojo.response.QueryState;
import com.tencent.supersonic.chat.core.query.plugin.WebBase;
import com.tencent.supersonic.chat.core.query.plugin.webpage.WebPageQuery;
import com.tencent.supersonic.chat.core.query.plugin.webpage.WebPageResp;
import org.junit.Assert;

public class BasePluginTest extends BaseApplication {

    protected void assertPluginRecognizeResult(QueryResult queryResult) {
        Assert.assertEquals(queryResult.getQueryState(), QueryState.SUCCESS);
        Assert.assertEquals(queryResult.getQueryMode(), WebPageQuery.QUERY_MODE);
        WebPageResp webPageResponse = (WebPageResp) queryResult.getResponse();
        WebBase webPage = webPageResponse.getWebPage();
        Assert.assertEquals(webPage.getUrl(), "www.yourbi.com");
        Assert.assertEquals(1, webPage.getParams().size());
        Assert.assertEquals("alice", webPage.getParams().get(0).getValue());
    }

}
