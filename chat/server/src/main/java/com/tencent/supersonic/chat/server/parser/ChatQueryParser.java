package com.tencent.supersonic.chat.server.parser;

import com.tencent.supersonic.chat.server.pojo.ParseContext;

public interface ChatQueryParser {

    boolean accept(ParseContext parseContext);

    void parse(ParseContext parseContext);
}
