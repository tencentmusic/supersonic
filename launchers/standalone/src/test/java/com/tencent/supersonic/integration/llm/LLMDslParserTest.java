package com.tencent.supersonic.integration.llm;

import static org.mockito.Mockito.when;

import com.tencent.supersonic.chat.api.component.SemanticParser;
import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import com.tencent.supersonic.chat.config.LLMConfig;
import com.tencent.supersonic.chat.parser.llm.dsl.LLMDslParser;
import com.tencent.supersonic.chat.utils.ComponentFactory;
import com.tencent.supersonic.integration.BaseQueryTest;
import com.tencent.supersonic.util.DataUtils;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.MockBean;

public class LLMDslParserTest extends BaseQueryTest {

    @MockBean
    protected LLMConfig llmConfig;

    @Test
    public void parse() throws Exception {
        String queryText = "周杰伦专辑十一月的萧邦有哪些歌曲";
        QueryReq queryReq = DataUtils.getQueryContextReq(10, queryText);
        QueryContext queryContext = new QueryContext();
        queryContext.setRequest(queryReq);
        SemanticParser dslParser = ComponentFactory.getSemanticParsers().stream().filter(parser -> {
                    if (parser instanceof LLMDslParser) {
                        return true;
                    } else {
                        return false;
                    }
                }
        ).findFirst().get();

        when(llmConfig.getUrl()).thenReturn("llmUrl");

        ChatContext chatCtx = new ChatContext();
        dslParser.parse(queryContext, chatCtx);
    }

}
