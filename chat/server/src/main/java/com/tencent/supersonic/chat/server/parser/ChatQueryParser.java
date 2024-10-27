package com.tencent.supersonic.chat.server.parser;

import com.tencent.supersonic.chat.server.pojo.ParseContext;

public interface ChatQueryParser {

    void parse(ParseContext parseContext);
}
