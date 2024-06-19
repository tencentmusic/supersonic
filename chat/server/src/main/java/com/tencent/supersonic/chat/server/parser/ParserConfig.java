package com.tencent.supersonic.chat.server.parser;

import com.google.common.collect.Lists;
import com.tencent.supersonic.common.config.ParameterConfig;
import com.tencent.supersonic.common.pojo.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("ChatParserConfig")
@Slf4j
public class ParserConfig extends ParameterConfig {

    public static final Parameter PARSER_MULTI_TURN_ENABLE =
            new Parameter("s2.parser.multi-turn.enable", "false",
                    "是否开启多轮对话", "开启多轮对话将消耗更多token",
                    "bool", "Parser相关配置");

    @Override
    public List<Parameter> getSysParameters() {
        return Lists.newArrayList(
                PARSER_MULTI_TURN_ENABLE
        );
    }

}
