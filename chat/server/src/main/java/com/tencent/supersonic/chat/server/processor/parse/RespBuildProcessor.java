package com.tencent.supersonic.chat.server.processor.parse;

import com.tencent.supersonic.chat.server.pojo.ChatParseContext;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.response.ParseResp;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

/**
 * RespBuildProcessor fill response object with parsing results.
 **/
@Slf4j
public class RespBuildProcessor implements ParseResultProcessor {

    @Override
    public void process(ChatParseContext chatParseContext, ParseResp parseResp) {
        parseResp.setChatId(chatParseContext.getChatId());
        parseResp.setQueryText(chatParseContext.getQueryText());
        List<SemanticParseInfo> parseInfos = parseResp.getSelectedParses();
        if (CollectionUtils.isNotEmpty(parseInfos)) {
            parseResp.setState(ParseResp.ParseState.COMPLETED);
        } else {
            parseResp.setState(ParseResp.ParseState.FAILED);
        }
    }
}
