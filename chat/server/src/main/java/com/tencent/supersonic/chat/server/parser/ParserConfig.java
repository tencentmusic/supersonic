package com.tencent.supersonic.chat.server.parser;

import com.tencent.supersonic.common.config.ParameterConfig;
import com.tencent.supersonic.common.pojo.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service("ChatQueryParserConfig")
@Slf4j
public class ParserConfig extends ParameterConfig {

    public static final Parameter PARSER_MULTI_TURN_ENABLE =
            new Parameter("s2.parser.multi-turn.enable", "false", "是否开启多轮对话", "开启多轮对话将消耗更多token",
                    "bool", "语义解析配置");
}
